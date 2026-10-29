package com.frnc.ancient_remnants_add.mixin;

import com.frnc.ancient_remnants_add.altar.AltarBoostScope;
import com.frnc.ancient_remnants_add.altar.AltarHarvestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 甜浆果是原版里少数「右键收割而不是破坏方块」的作物。
 * 在祭坛丰收生效范围内（与祭坛层级一致），右键收获成熟甜浆果也按丰收概率额外掉一份。
 */
@Mixin(SweetBerryBushBlock.class)
public abstract class MixinSweetBerryBushAltar {

    @Inject(method = "use", at = @At("HEAD"))
    private void ancientRemnantsAdd$extraSweetBerries(
            BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int age = state.getValue(SweetBerryBushBlock.AGE);
        if (age <= 1) {
            return;
        }
        // 照抄原版 use 的骨粉分支：age<3 且手持骨粉时原版直接 PASS（不收割），这里也不该掉东西。
        if (age < 3 && player.getItemInHand(hand).is(Items.BONE_MEAL)) {
            return;
        }
        // 不需要与「个人丰收祝福」去重：上游的丰收祝福只挂在 Block#playerDestroy 上
        // （见上游 ancient_remnants.mixins.json 的 MixinBlock），而右键收割走的是
        // SweetBerryBushBlock#use，不经过 getDrops/playerDestroy，所以不会重复掉落。
        if (!AltarBoostScope.harvestEnabledAt(serverLevel, pos)) {
            return;
        }
        if (!AltarHarvestHelper.rollChance(serverLevel)) {
            return;
        }
        int count = 1 + level.random.nextInt(2) + (age == 3 ? 1 : 0);
        Block.popResourceFromFace(level, pos, hit.getDirection(), new ItemStack(Items.SWEET_BERRIES, count));
    }
}
