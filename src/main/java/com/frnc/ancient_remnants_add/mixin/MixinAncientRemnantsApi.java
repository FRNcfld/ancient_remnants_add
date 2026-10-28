package com.frnc.ancient_remnants_add.mixin;

import com.frnc.ancient_remnants_add.blessing.IMultiBlessingPlayer;
import com.frnc.ancient_remnants_add.blessing.MultiBlessings;
import dev.obscuria.ancient_remnants.api.AncientRemnantsApi;
import dev.obscuria.ancient_remnants.common.Blessing;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把上游所有「设置祝福」的入口接管到多祝福列表。
 *
 * <p>上游的 Elderheart（方尖碑/纪念碑）和 {@code /ancient_remnants blessing set}
 * 命令最终都调用 {@code AncientRemnantsApi.setBlessing(Player, Holder)}，在这里拦一次：
 * 非 null 时加入列表，满额时替换最早获得的那个。
 *
 * <p>{@code null} 故意不在这里清空列表：上游的 {@code Blessing.onRestore}
 * 在旧玩家没有单祝福时也会调用 {@code setBlessing(null)}，如果这里清空，
 * 重生时会把刚复制的多祝福列表一起抹掉。命令 remove 的清空逻辑由
 * {@link MixinBlessingCommands} 单独处理。
 *
 * <p>被取消的原始实现负责更新上游单祝福字段并移除/添加效果；本 mod 在
 * {@link MultiBlessings} 里通过 {@code IPlayerAccess} 直接调用同一套逻辑，
 * 避免递归回到本注入点。
 */
@Mixin(AncientRemnantsApi.class)
public abstract class MixinAncientRemnantsApi {

    @Inject(method = "setBlessing", at = @At("HEAD"), cancellable = true, remap = false)
    private void ancientRemnantsAdd$setBlessing(Player player, Holder<Blessing> blessing, CallbackInfo ci) {
        if (blessing == null) {
            return;
        }
        if (!(player instanceof IMultiBlessingPlayer access)) {
            return;
        }
        MultiBlessings.acquire(player, access, blessing);
        ci.cancel();
    }

    /** 公开 API 的 resetBlessing 也按「清空全部」处理。 */
    @Inject(method = "resetBlessing", at = @At("HEAD"), cancellable = true, remap = false)
    private void ancientRemnantsAdd$resetBlessing(Player player, CallbackInfo ci) {
        if (player instanceof IMultiBlessingPlayer access) {
            MultiBlessings.clear(player, access);
            ci.cancel();
        }
    }
}
