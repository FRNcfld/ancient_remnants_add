package com.frnc.ancient_remnants_add.blessing;

/**
 * 由 {@code MixinPlayerBlessings} 注入到 Player 上的绝境活力状态。
 *
 * <p>冷却、buff 结束时间和触发前的吸收值都会写入玩家 NBT，并在重生时复制；
 * 这样重新登录后黄心到期时仍能正确恢复玩家原有的吸收值。
 */
public interface IDesperateVitalityAccess {

    /** 冷却结束时间在玩家 NBT 中的键。 */
    String COOLDOWN_NBT_KEY = "AncientRemnantsAddDesperateVitalityCooldown";

    /** 临时吸收 buff 结束时间在玩家 NBT 中的键。 */
    String BUFF_END_NBT_KEY = "AncientRemnantsAddDesperateVitalityBuffEnd";

    /** 触发前吸收值在玩家 NBT 中的键。 */
    String PREVIOUS_ABSORPTION_NBT_KEY = "AncientRemnantsAddDesperateVitalityPreviousAbsorption";

    /** 冷却结束的游戏时间；0 表示可用。 */
    long ancientRemnantsAdd$getDesperateVitalityCooldownEnd();

    void ancientRemnantsAdd$setDesperateVitalityCooldownEnd(long gameTime);

    /** 临时吸收 buff 的结束游戏时间；0 表示当前没有 buff。 */
    long ancientRemnantsAdd$getDesperateVitalityBuffEnd();

    void ancientRemnantsAdd$setDesperateVitalityBuffEnd(long gameTime);

    /** 触发前玩家已有的吸收值，用于 buff 结束时只移除本祝福提供的部分。 */
    double ancientRemnantsAdd$getDesperateVitalityPreviousAbsorption();

    void ancientRemnantsAdd$setDesperateVitalityPreviousAbsorption(double absorption);
}
