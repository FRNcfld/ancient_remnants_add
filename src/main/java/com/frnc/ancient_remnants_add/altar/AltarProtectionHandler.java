package com.frnc.ancient_remnants_add.altar;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 激活后的祭坛结构保护。
 *
 * <p>覆盖玩家破坏、爆炸和活塞；保护范围由 {@link AltarSavedData#isProtected} 按
 * 激活站点的 32×25×32 区域判断，不扫描整块区域。
 */
@Mod.EventBusSubscriber(modid = AncientRemnantsAdd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AltarProtectionHandler {

    private AltarProtectionHandler() {
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (AltarSavedData.get(level).isProtected(event.getPos())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        AltarSavedData data = AltarSavedData.get(level);
        if (!data.hasActiveSite()) {
            return;
        }
        // 保留爆炸伤害；只从方块破坏列表里移除结构区域。
        event.getAffectedBlocks().removeIf(data::isProtected);
    }

    @SubscribeEvent
    public static void onPiston(PistonEvent.Pre event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        AltarSavedData data = AltarSavedData.get(level);
        if (!data.hasActiveSite()) {
            return;
        }
        PistonStructureResolver helper = event.getStructureHelper();
        if (helper == null) {
            return;
        }
        if (!helper.resolve()) {
            // 收回时活塞头会先被移除；resolve 失败时单独检查它。
            if (!event.getPistonMoveType().isExtend && data.isProtected(event.getFaceOffsetPos())) {
                event.setCanceled(true);
            }
            return;
        }
        for (BlockPos pos : helper.getToPush()) {
            if (data.isProtected(pos)) {
                event.setCanceled(true);
                return;
            }
        }
        for (BlockPos pos : helper.getToDestroy()) {
            if (data.isProtected(pos)) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
