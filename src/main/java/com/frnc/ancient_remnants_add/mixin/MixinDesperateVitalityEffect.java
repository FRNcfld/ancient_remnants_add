package com.frnc.ancient_remnants_add.mixin;

import dev.obscuria.ancient_remnants.common.effect.DesperateVitalityEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 关闭绝境活力原本的「低血量给再生」逻辑。
 *
 * <p>新机制（临时最大生命 + 冷却）由
 * {@link com.frnc.ancient_remnants_add.blessing.DesperateVitalityHandler} 在服务端
 * 玩家 tick 里实现；这里让旧的 {@code tickBlessing} 永远不执行。
 */
@Mixin(DesperateVitalityEffect.class)
public abstract class MixinDesperateVitalityEffect {

    @Inject(method = "shouldTickBlessing", at = @At("HEAD"), cancellable = true, remap = false)
    private void ancientRemnantsAdd$disableLegacyRegeneration(
            int tickCount, int amplifier, CallbackInfoReturnable<Boolean> cir
    ) {
        cir.setReturnValue(false);
    }
}
