package com.frnc.ancient_remnants_add.mixin;

import com.frnc.ancient_remnants_add.Config;
import com.frnc.ancient_remnants_add.altar.AltarBoostScope;
import com.frnc.ancient_remnants_add.altar.AltarHarvestHelper;
import com.frnc.ancient_remnants_add.blessing.BlessingIds;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 丰收：把「50% 概率额外掉一份」改为配置概率，并叠加祭坛祝福倍率。
 *
 * <p>上游 {@code BountifulHarvestEffect.Companion.shouldTrigger(Player)} 先检查玩家是否有
 * 丰收效果，再掷 {@code nextBoolean()}。这里在 HEAD 接管整个判定：保留效果检查，
 * 概率改为 {@code 配置概率 × 祭坛倍率}，封顶 1.0。
 */
@Mixin(targets = "dev.obscuria.ancient_remnants.common.effect.BountifulHarvestEffect$Companion")
public abstract class MixinBountifulHarvestEffectCompanion {

    @Inject(method = "shouldTrigger", at = @At("HEAD"), cancellable = true, remap = false)
    private void ancientRemnantsAdd$rollChance(Player player, CallbackInfoReturnable<Boolean> cir) {
        MobEffect effect = AltarHarvestHelper.bountifulHarvestEffect();
        if (effect == null || !player.hasEffect(effect)) {
            cir.setReturnValue(false);
            return;
        }
        double chance = Config.bountifulHarvestChance()
                * AltarBoostScope.multiplierFor(player, BlessingIds.BOUNTIFUL_HARVEST);
        cir.setReturnValue(player.getRandom().nextFloat() < (float) Math.min(1.0D, chance));
    }
}
