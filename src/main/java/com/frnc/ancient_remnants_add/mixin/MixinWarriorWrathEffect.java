package com.frnc.ancient_remnants_add.mixin;

import com.frnc.ancient_remnants_add.Config;
import dev.obscuria.ancient_remnants.common.effect.WarriorWrathEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 战士之怒：怪物计数半径、满增伤所需怪物数、最大增伤都改为配置值。
 *
 * <p>原实现：{@code boundingBox.inflate(12)} 统计怪物，
 * {@code clampedMap(count, 0, 12, 0, 1)} → 缓动 → {@code value.multiply(1 + bonus)}。
 * 与猎人本能一样，只替换距离/数量上限，并在乘法前缩放额外伤害比例。
 */
@Mixin(WarriorWrathEffect.class)
public abstract class MixinWarriorWrathEffect {

    @ModifyArg(
            method = "modifyOutgoingDamage",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/AABB;inflate(D)Lnet/minecraft/world/phys/AABB;"
            ),
            index = 0,
            remap = false
    )
    private double ancientRemnantsAdd$monsterRadius(double radius) {
        return Config.warriorWrathMonsterRadius();
    }

    @ModifyArg(
            method = "modifyOutgoingDamage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clampedMap(FFFFF)F"),
            index = 2,
            remap = false
    )
    private float ancientRemnantsAdd$maxMonsters(float maxMonsters) {
        return (float) Config.warriorWrathMaxMonsters();
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
        return 1.0F + (multiplier - 1.0F) * (float) Config.warriorWrathMaxDamageBonus();
    }
}
