package com.frnc.ancient_remnants_add.mixin;

import com.frnc.ancient_remnants_add.Config;
import dev.obscuria.ancient_remnants.common.effect.SentinelAegisEffect;
import dev.obscuria.ancient_remnants.common.effect.Value;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 哨兵护盾的新规则：每损失 1% 生命值，本次伤害计算使用的护甲值提高 1%。
 *
 * <p>原实现是「低血量时额外加固定 20 点护甲」；现在改为按缺失生命比例直接放大
 * 传入的护甲值：
 * {@code 护甲值 × (1 + 缺失生命比例 × 配置倍率)}。
 * 默认配置倍率 1.0，即满血无加成、损失 50% 生命时护甲 ×1.5、损失 100% 时 ×2.0。
 * 如果玩家本身护甲为 0，乘算仍然是 0。
 */
@Mixin(SentinelAegisEffect.class)
public abstract class MixinSentinelAegisEffect {

    @Inject(method = "modifyArmorValue", at = @At("HEAD"), cancellable = true, remap = false)
    private void ancientRemnantsAdd$missingHealthArmor(
            LivingEntity self, DamageSource source, Value value, CallbackInfo ci
    ) {
        float missing = 1.0F - self.getHealth() / self.getMaxHealth();
        missing = Math.max(0.0F, Math.min(1.0F, missing));
        value.multiply(1.0F + missing * (float) Config.sentinelAegisArmorBonusPerHealthPercent());
        ci.cancel();
    }
}
