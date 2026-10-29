package com.frnc.ancient_remnants_add.blessing;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.frnc.ancient_remnants_add.Config;
import com.frnc.ancient_remnants_add.altar.AltarBoostScope;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 绝境活力的新机制（黄色吸收心，不改变最大生命值）：
 *
 * <ul>
 *   <li>生命值低于阈值（默认 50%）时，获得一段临时吸收生命（默认最大生命的 50%）；</li>
 *   <li>吸收生命持续 10 秒，期间优先代替生命值承受伤害；</li>
 *   <li>buff 结束后进入 5 分钟冷却，冷却从 buff 结束时开始计算；</li>
 *   <li>冷却、buff 结束时间和触发前的吸收值都会持久化，重登/重生后到期时
 *       只移除本祝福提供的部分，不会把玩家原本的吸收心一起清掉。</li>
 * </ul>
 *
 * <p>旧版 {@code DesperateVitalityEffect} 的再生逻辑已由 mixin 关闭，这里通过玩家 tick
 * 驱动新机制。实现直接设置原版 {@code LivingEntity} 的吸收值，因此会显示为黄色心。
 */
public final class DesperateVitalityHandler {

    private static final ResourceLocation DESPERATE_VITALITY =
            new ResourceLocation(AncientRemnantsAdd.ANCIENT_REMNANTS_MOD_ID, "desperate_vitality");

    private static MobEffect desperateVitalityEffect;

    private DesperateVitalityHandler() {
    }

    public static void tick(ServerPlayer player) {
        if (!(player instanceof IDesperateVitalityAccess access)) {
            // mixin 没生效时安静退出，不在玩家 tick 里抛 ClassCastException。
            return;
        }
        long now = player.level().getGameTime();
        long buffEnd = access.ancientRemnantsAdd$getDesperateVitalityBuffEnd();

        if (buffEnd > 0L) {
            if (now < buffEnd) {
                return;
            }

            // buff 到期：只移除本祝福提供的吸收部分，保留触发前已有的吸收值。
            double previous = access.ancientRemnantsAdd$getDesperateVitalityPreviousAbsorption();
            player.setAbsorptionAmount((float) Math.max(
                    0.0D,
                    Math.min(player.getAbsorptionAmount(), previous)
            ));
            access.ancientRemnantsAdd$setDesperateVitalityBuffEnd(0L);
            access.ancientRemnantsAdd$setDesperateVitalityPreviousAbsorption(0.0D);
        }

        MobEffect effect = effect();
        if (effect == null || !player.hasEffect(effect)) {
            return;
        }
        if (player.getHealth() >= player.getMaxHealth() * Config.desperateVitalityHealthThreshold()) {
            return;
        }
        if (now < access.ancientRemnantsAdd$getDesperateVitalityCooldownEnd()) {
            return;
        }

        double altar = AltarBoostScope.multiplierFor(player, BlessingIds.DESPERATE_VITALITY);
        double absorption = player.getMaxHealth() * Config.desperateVitalityAbsorptionHealthPercentage() * altar;
        if (absorption <= 0.0D) {
            return;
        }

        double previous = player.getAbsorptionAmount();
        player.setAbsorptionAmount((float) (previous + absorption));

        long duration = Config.desperateVitalityDuration();
        long cooldown = Config.desperateVitalityCooldown();
        access.ancientRemnantsAdd$setDesperateVitalityBuffEnd(now + duration);
        access.ancientRemnantsAdd$setDesperateVitalityPreviousAbsorption(previous);
        // 冷却从 buff 结束时开始计算。
        access.ancientRemnantsAdd$setDesperateVitalityCooldownEnd(now + duration + cooldown);
    }

    private static MobEffect effect() {
        if (desperateVitalityEffect == null) {
            desperateVitalityEffect = ForgeRegistries.MOB_EFFECTS.getValue(DESPERATE_VITALITY);
        }
        return desperateVitalityEffect;
    }
}
