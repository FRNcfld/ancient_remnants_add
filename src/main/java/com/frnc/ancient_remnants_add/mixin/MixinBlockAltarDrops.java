package com.frnc.ancient_remnants_add.mixin;

import com.frnc.ancient_remnants_add.altar.AltarHarvestHelper;
import com.frnc.ancient_remnants_add.altar.AltarSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 远古之心激活后，所在维度的所有作物破坏/收割都按丰收概率额外掉一份。
 *
 * <p>挂在 {@link Block#getDrops} 上可以同时覆盖玩家破坏、活塞/水流/爆炸、
 * Create 收割机等一切走原版战利品掉落的路径。玩家自己带丰收祝福且破坏的是
 * 成熟的 CropBlock / NetherWart 时跳过，交给上游原本的 playerDestroy hook，
 * 避免同一次破坏触发两次。
 */
@Mixin(Block.class)
public abstract class MixinBlockAltarDrops {

    @Inject(
            method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void ancientRemnantsAdd$altarExtraDrops(
            BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity,
            Entity entity, ItemStack tool, CallbackInfoReturnable<List<ItemStack>> cir
    ) {
        if (!AltarHarvestHelper.isHarvestable(state)) {
            return;
        }
        if (!AltarSavedData.get(level).isActive(level)) {
            return;
        }
        if (AltarHarvestHelper.shouldSkipForPersonalBlessing(entity, state)) {
            return;
        }
        if (!AltarHarvestHelper.rollChance(level)) {
            return;
        }
        List<ItemStack> drops = cir.getReturnValue();
        if (drops == null || drops.isEmpty()) {
            return;
        }
        cir.setReturnValue(AltarHarvestHelper.duplicateDrops(drops));
    }
}
