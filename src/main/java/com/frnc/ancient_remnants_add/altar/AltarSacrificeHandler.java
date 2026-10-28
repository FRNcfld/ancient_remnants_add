package com.frnc.ancient_remnants_add.altar;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 激活祭坛后的献祭：玩家在祭坛范围内击杀名单内生物 → 摧毁远古之心、熄灭祭坛、
 * 把保存的典籍掉在祭坛中心；站点保留，可重新修复。
 *
 * <p>名单是实体类型标签 {@code ancient_remnants_add:altar_sacrifices}，默认只有村民。
 */
@Mod.EventBusSubscriber(modid = AncientRemnantsAdd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AltarSacrificeHandler {

    public static final TagKey<EntityType<?>> ALTAR_SACRIFICES =
            TagKey.create(Registries.ENTITY_TYPE, AncientRemnantsAdd.id("altar_sacrifices"));

    private AltarSacrificeHandler() {
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        // 只认玩家击杀；弹射物的 causing entity 会是玩家，宠物/环境击杀不算。
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) {
            return;
        }
        if (!event.getEntity().getType().is(ALTAR_SACRIFICES)) {
            return;
        }

        AltarSavedData data = AltarSavedData.get(level);
        AltarSavedData.SiteRecord site = data.findActiveSiteAt(event.getEntity().blockPosition());
        if (site == null) {
            return;
        }

        BlockPos root = site.root();
        ItemStack codex = data.extinguish(root);
        // 直接移除方块：不经过 playerDestroy，因此不会掉远古之心物品。
        // 方块 onRemove 会清理其余三格；此时站点已熄灭，典籍不会再掉第二次。
        level.removeBlock(root, false);
        if (!codex.isEmpty()) {
            Block.popResource(level, root.offset(1, 1, 1), codex);
        }
        level.playSound(null, root, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
        killer.displayClientMessage(
                Component.translatable("message.ancient_remnants_add.altar.sacrificed"), false);
    }
}
