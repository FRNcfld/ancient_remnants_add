package com.frnc.ancient_remnants_add.mixin;

import com.frnc.ancient_remnants_add.Config;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 丰收：把「50% 概率额外掉一份」改为配置概率。
 *
 * <p>{@code BountifulHarvestEffect.Companion.shouldTrigger} 原本直接调用
 * {@code RandomSource.nextBoolean()}；这里换成 {@code nextFloat() < 配置概率}，
 * 因此 0 / 0.5 / 1 分别对应永不 / 原版 / 必定触发。
 * 之后若有其他来源（例如多方块祭坛）调用同一个 {@code shouldTrigger}，
 * 也会自动使用这个配置。
 */
@Mixin(targets = "dev.obscuria.ancient_remnants.common.effect.BountifulHarvestEffect$Companion")
public abstract class MixinBountifulHarvestEffectCompanion {

    @Redirect(
            method = "shouldTrigger",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextBoolean()Z"),
            remap = false
    )
    private boolean ancientRemnantsAdd$rollChance(RandomSource random) {
        return random.nextFloat() < (float) Config.bountifulHarvestChance();
    }
}
