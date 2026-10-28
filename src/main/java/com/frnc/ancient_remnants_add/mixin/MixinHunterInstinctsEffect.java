package com.frnc.ancient_remnants_add.mixin;

import com.frnc.ancient_remnants_add.Config;
import dev.obscuria.ancient_remnants.common.effect.HunterInstinctsEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 猎人本能：距离上限与最大增伤改为配置值。
 *
 * <p>原实现：{@code clampedMap(distance, 0, 64, 0, 1)} 得到 0-1 的比例，
 * {@code Easing.EASE_IN_CIRCLE.apply(...)} 缓动后作为「额外伤害比例」，
 * 最终 {@code value.multiply(1 + bonus)}。
 * 这里只替换距离上限，并在 {@code Value.multiply} 前缩放额外比例，
 * 因此缓动曲线和配置大于 100% 的情况都能保持正确。
 */
@Mixin(HunterInstinctsEffect.class)
public abstract class MixinHunterInstinctsEffect {

    @ModifyArg(
            method = "modifyOutgoingDamage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clampedMap(FFFFF)F"),
            index = 2,
            remap = false
    )
    private float ancientRemnantsAdd$maxDistance(float maxDistance) {
        return (float) Config.hunterInstinctsMaxDistance();
    }

    @ModifyArg(
            method = "modifyOutgoingDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/obscuria/ancient_remnants/common/effect/Value;multiply(F)Ldev/obscuria/ancient_remnants/common/effect/Value;",
                    remap = false
            ),
            index = 0,
            remap = false
    )
    private float ancientRemnantsAdd$maxDamageBonus(float multiplier) {
        return 1.0F + (multiplier - 1.0F) * (float) Config.hunterInstinctsMaxDamageBonus();
    }
}
