package com.frnc.ancient_remnants_add.altar;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 祭坛仪式：在祭坛范围内 Shift + 长按右键 2 秒，消耗完整典籍并激活。
 *
 * <p>不新增网络包：客户端按住右键时会每 4 tick 发送一次 {@code ServerboundUseItemPacket}。
 * 客户端在 {@link PlayerInteractEvent.RightClickItem} 取消事件以阻止典籍界面打开（包仍会发送），
 * 服务端在同一个事件里计数；Shift 是必需条件，不按 Shift 时典籍界面照常打开。
 */
@Mod.EventBusSubscriber(modid = AncientRemnantsAdd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AltarActivationHandler {

    /** 2 秒；客户端使用包约每 4 tick 一次。 */
    private static final int HOLD_TICKS = 40;
    /** 松手/离开后超过这个间隔就重置。 */
    private static final int GAP_TICKS = 10;
    private static final int MESSAGE_COOLDOWN_TICKS = 20;

    private record Hold(ServerLevel level, BlockPos root, long startTick, long lastTick) {
    }

    /** 服务端主线程使用；客户端不会写入。 */
    private static final Map<UUID, Hold> HOLDS = new HashMap<>();
    private static final Map<UUID, Long> LAST_MESSAGE = new HashMap<>();

    private AltarActivationHandler() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        boolean channel = player.isShiftKeyDown() && AncientEchoAdvancement.isCodex(stack);

        if (!channel) {
            if (!player.level().isClientSide) {
                HOLDS.remove(player.getUUID());
            }
            return;
        }

        if (player.level().isClientSide) {
            // 阻止 MonolithCodexItem.use() 打开典籍界面。
            // 注意：MultiPlayerGameMode.useItem 的 startPrediction 无论事件是否取消都会
            // 构造并发送 ServerboundUseItemPacket，所以服务端仍能收到长按脉冲。
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ServerLevel level = serverPlayer.serverLevel();
        long now = level.getGameTime();
        AltarSavedData data = AltarSavedData.get(level);
        BlockPos playerPos = serverPlayer.blockPosition();
        AltarSavedData.SiteRecord site = data.findSiteAt(playerPos);

        // 兼容站点记录已存在但 repaired 尚未写入的情况：2×2 完整即视为已修复。
        if (site != null && !site.repaired() && AncientHeartBlock.isComplete(level, site.root())) {
            data.markRepaired(site.root());
        }
        if (site == null || !site.repaired() || site.active()
                || !AncientHeartBlock.isComplete(level, site.root())) {
            HOLDS.remove(serverPlayer.getUUID());
            return;
        }

        if (!AncientEchoAdvancement.isCompleteCodex(serverPlayer, stack)) {
            HOLDS.remove(serverPlayer.getUUID());
            long last = LAST_MESSAGE.getOrDefault(serverPlayer.getUUID(), Long.MIN_VALUE);
            if (now - last >= MESSAGE_COOLDOWN_TICKS) {
                serverPlayer.displayClientMessage(
                        Component.translatable("message.ancient_remnants_add.altar.incomplete"), true);
                LAST_MESSAGE.put(serverPlayer.getUUID(), now);
            }
            return;
        }

        Hold previous = HOLDS.get(serverPlayer.getUUID());
        if (previous == null
                || !previous.root().equals(site.root())
                || now - previous.lastTick() > GAP_TICKS) {
            HOLDS.put(serverPlayer.getUUID(), new Hold(level, site.root(), now, now));
            return;
        }
        if (now - previous.startTick() >= HOLD_TICKS) {
            HOLDS.remove(serverPlayer.getUUID());
            completeActivation(serverPlayer, level, site.root(), event.getHand());
        } else {
            HOLDS.put(serverPlayer.getUUID(), new Hold(level, site.root(), previous.startTick(), now));
        }
    }

    private static void completeActivation(ServerPlayer player, ServerLevel level, BlockPos root,
                                           InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!AncientEchoAdvancement.isCompleteCodex(player, held)) {
            return;
        }
        if (!AncientHeartBlock.isComplete(level, root)) {
            return;
        }
        AltarSavedData data = AltarSavedData.get(level);
        AltarSavedData.SiteRecord site = data.findSiteAt(root);
        if (site == null || !site.repaired() || site.active()) {
            return;
        }

        ItemStack stored = held.copyWithCount(1);
        if (!data.activate(root, stored)) {
            return;
        }
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        // 仅补发原进度；激活本身只要求典籍完整。
        AncientEchoAdvancement.tryAward(player);
        AncientHeartBlock.setActive(level, root, true);
        // 激活时把祭坛结构修复为模板，并清掉结构内的无关方块/非生物实体；中央 2×2 保留。
        AltarRepair.repair(level, root);
        AltarRepair.clearEntities(level, root);
        AltarAdvancements.awardGloryReborn(player);

        level.playSound(null, root, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.0F);
        player.displayClientMessage(
                Component.translatable("message.ancient_remnants_add.altar.activated"), false);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        long now = server.overworld().getGameTime();
        Iterator<Map.Entry<UUID, Hold>> iterator = HOLDS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Hold> entry = iterator.next();
            Hold hold = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null
                    || !player.isAlive()
                    || player.level() != hold.level()
                    || now - hold.lastTick() > GAP_TICKS) {
                iterator.remove();
            }
        }
        if (LAST_MESSAGE.size() > 64) {
            LAST_MESSAGE.entrySet().removeIf(entry -> now - entry.getValue() > 200);
        }
    }
}
