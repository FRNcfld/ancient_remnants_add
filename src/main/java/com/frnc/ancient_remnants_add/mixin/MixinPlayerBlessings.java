package com.frnc.ancient_remnants_add.mixin;

import com.frnc.ancient_remnants_add.blessing.BlessingNbt;
import com.frnc.ancient_remnants_add.blessing.IDesperateVitalityAccess;
import com.frnc.ancient_remnants_add.blessing.IMultiBlessingPlayer;
import dev.obscuria.ancient_remnants.common.Blessing;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 在 Player 上保存多祝福列表与绝境活力状态。
 *
 * <p>祝福列表按获得顺序排列（最旧在前），由 {@code readAdditionalSaveData} /
 * {@code addAdditionalSaveData} 持久化；上游自己的单祝福 NBT 键不动。
 * 绝境活力持久化冷却结束时间、临时吸收 buff 的结束时间和触发前吸收值。
 */
@Mixin(Player.class)
public abstract class MixinPlayerBlessings implements IMultiBlessingPlayer, IDesperateVitalityAccess {

    @Unique
    private List<Holder<Blessing>> ancientRemnantsAdd$blessings;

    @Unique
    private long ancientRemnantsAdd$desperateVitalityCooldownEnd;

    @Unique
    private long ancientRemnantsAdd$desperateVitalityBuffEnd;

    @Unique
    private double ancientRemnantsAdd$desperateVitalityPreviousAbsorption;

    @Override
    public List<Holder<Blessing>> ancientRemnantsAdd$getBlessings() {
        if (ancientRemnantsAdd$blessings == null) {
            ancientRemnantsAdd$blessings = new ArrayList<>();
        }
        return ancientRemnantsAdd$blessings;
    }

    @Override
    public void ancientRemnantsAdd$setBlessings(List<Holder<Blessing>> blessings) {
        List<Holder<Blessing>> target = ancientRemnantsAdd$getBlessings();
        // 先复制：允许调用方把 getBlessings() 返回的同一个列表传回来。
        List<Holder<Blessing>> copy = new ArrayList<>(blessings);
        target.clear();
        target.addAll(copy);
    }

    @Override
    public long ancientRemnantsAdd$getDesperateVitalityCooldownEnd() {
        return ancientRemnantsAdd$desperateVitalityCooldownEnd;
    }

    @Override
    public void ancientRemnantsAdd$setDesperateVitalityCooldownEnd(long gameTime) {
        ancientRemnantsAdd$desperateVitalityCooldownEnd = gameTime;
    }

    @Override
    public long ancientRemnantsAdd$getDesperateVitalityBuffEnd() {
        return ancientRemnantsAdd$desperateVitalityBuffEnd;
    }

    @Override
    public void ancientRemnantsAdd$setDesperateVitalityBuffEnd(long gameTime) {
        ancientRemnantsAdd$desperateVitalityBuffEnd = gameTime;
    }

    @Override
    public double ancientRemnantsAdd$getDesperateVitalityPreviousAbsorption() {
        return ancientRemnantsAdd$desperateVitalityPreviousAbsorption;
    }

    @Override
    public void ancientRemnantsAdd$setDesperateVitalityPreviousAbsorption(double absorption) {
        ancientRemnantsAdd$desperateVitalityPreviousAbsorption = absorption;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void ancientRemnantsAdd$saveBlessings(CompoundTag tag, CallbackInfo ci) {
        List<Holder<Blessing>> blessings = ancientRemnantsAdd$getBlessings();
        if (!blessings.isEmpty()) {
            BlessingNbt.CODEC.encodeStart(NbtOps.INSTANCE, blessings)
                    .result()
                    .ifPresent(encoded -> tag.put(BlessingNbt.KEY, encoded));
        }
        if (ancientRemnantsAdd$desperateVitalityCooldownEnd > 0) {
            tag.putLong(IDesperateVitalityAccess.COOLDOWN_NBT_KEY, ancientRemnantsAdd$desperateVitalityCooldownEnd);
        }
        if (ancientRemnantsAdd$desperateVitalityBuffEnd > 0) {
            tag.putLong(IDesperateVitalityAccess.BUFF_END_NBT_KEY, ancientRemnantsAdd$desperateVitalityBuffEnd);
            tag.putDouble(
                    IDesperateVitalityAccess.PREVIOUS_ABSORPTION_NBT_KEY,
                    ancientRemnantsAdd$desperateVitalityPreviousAbsorption
            );
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void ancientRemnantsAdd$loadBlessings(CompoundTag tag, CallbackInfo ci) {
        if (tag.contains(BlessingNbt.KEY)) {
            BlessingNbt.CODEC.parse(NbtOps.INSTANCE, tag.get(BlessingNbt.KEY))
                    .result()
                    .ifPresent(decoded -> ancientRemnantsAdd$setBlessings(decoded));
        }
        if (tag.contains(IDesperateVitalityAccess.COOLDOWN_NBT_KEY)) {
            ancientRemnantsAdd$desperateVitalityCooldownEnd =
                    tag.getLong(IDesperateVitalityAccess.COOLDOWN_NBT_KEY);
        }
        if (tag.contains(IDesperateVitalityAccess.BUFF_END_NBT_KEY)) {
            ancientRemnantsAdd$desperateVitalityBuffEnd =
                    tag.getLong(IDesperateVitalityAccess.BUFF_END_NBT_KEY);
            ancientRemnantsAdd$desperateVitalityPreviousAbsorption =
                    tag.getDouble(IDesperateVitalityAccess.PREVIOUS_ABSORPTION_NBT_KEY);
        }
    }
}
