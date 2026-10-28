package com.frnc.ancient_remnants_add.mixin;

import com.frnc.ancient_remnants_add.blessing.IDesperateVitalityAccess;
import com.frnc.ancient_remnants_add.blessing.IMultiBlessingPlayer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 死亡/重生、跨维度切换时把多祝福列表复制到新的 ServerPlayer。
 *
 * <p>上游的 {@code MixinServerPlayer} 只复制单祝福字段；这里补上本 mod 的列表。
 * 两边都在 HEAD 注入，先后顺序不影响结果：列表复制是整体覆盖，重复项也会在
 * 下一 tick 的 {@code MultiBlessings.sync} 里去掉。
 */
@Mixin(ServerPlayer.class)
public abstract class MixinServerPlayerBlessings {

    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void ancientRemnantsAdd$copyBlessings(ServerPlayer oldPlayer, boolean restoreAll, CallbackInfo ci) {
        if (oldPlayer instanceof IMultiBlessingPlayer oldBlessings) {
            ((IMultiBlessingPlayer) (Object) this)
                    .ancientRemnantsAdd$setBlessings(oldBlessings.ancientRemnantsAdd$getBlessings());
        }
        if (oldPlayer instanceof IDesperateVitalityAccess oldState) {
            IDesperateVitalityAccess newState = (IDesperateVitalityAccess) (Object) this;
            newState.ancientRemnantsAdd$setDesperateVitalityCooldownEnd(
                    oldState.ancientRemnantsAdd$getDesperateVitalityCooldownEnd());
            newState.ancientRemnantsAdd$setDesperateVitalityBuffEnd(
                    oldState.ancientRemnantsAdd$getDesperateVitalityBuffEnd());
            newState.ancientRemnantsAdd$setDesperateVitalityPreviousAbsorption(
                    oldState.ancientRemnantsAdd$getDesperateVitalityPreviousAbsorption());
        }
    }
}
