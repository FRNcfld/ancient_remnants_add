package com.frnc.ancient_remnants_add;

import com.frnc.ancient_remnants_add.blessing.BlessingTuning;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 本 mod 的通用配置（{@code config/ancient_remnants_add-common.toml}）。
 *
 * <p>当前有调试开关、祝福数量上限，以及六个祝福的数值（{@code [blessings]} 段）。
 * 加新选项时注意四条：
 * <ol>
 *   <li>取值只放在访问器的方法体里（像 {@link #debugLogging()} 这样），不要写成
 *       {@code static final} 字段——配置的加载时机晚于方块/物品的构造；</li>
 *   <li>开发环境里 {@code ConfigValue#get()} 在配置未加载时会抛 {@code IllegalStateException}
 *       （生产环境才退回默认值），所以不要在加载期调用；</li>
 *   <li>{@code .comment()} 里不要出现 ASCII 双引号，改完同步更新 README 里的配置表；</li>
 *   <li>祝福数值在效果触发时实时读取，改完即时生效；建筑师之触的属性修正器由
 *       {@link BlessingTuning#applyArchitectReach()} 在配置加载/注册完成后刷新。</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = AncientRemnantsAdd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Whether to print this mod's debug logs to the game log.")
            .define("debugLogging", true);

    private static final ForgeConfigSpec.IntValue MAX_BLESSINGS = BUILDER
            .comment("Maximum number of Ancient Remnants blessings a player can hold at once.",
                    "When the limit is reached, a newly acquired blessing replaces the oldest one.")
            .defineInRange("maxBlessings", 6, 1, 64);

    private static final ForgeConfigSpec.DoubleValue ALTAR_BLESSING_BOOST_PER_ALTAR = BUILDER
            .comment("Blessing effect boost per active altar. 1.0 means +100% per counted altar.",
                    "Stacking is additive and capped per blessing by data/ancient_remnants_add/altar_boost.json.",
                    "0 disables altar blessing boosts.")
            .defineInRange("altarBlessingBoostPerAltar", 1.0D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue BOUNTIFUL_HARVEST_CHANCE = BUILDER
            .push("blessings")
            .comment("Chance (0-1) for Bountiful Harvest to duplicate the drops of a fully grown crop.")
            .defineInRange("bountifulHarvestChance", 0.5D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue HUNTER_INSTINCTS_MAX_DISTANCE = BUILDER
            .comment("Distance in blocks over which Hunter Instincts scales up to its maximum damage bonus.")
            .defineInRange("hunterInstinctsMaxDistance", 64.0D, 1.0D, 256.0D);

    private static final ForgeConfigSpec.DoubleValue HUNTER_INSTINCTS_MAX_DAMAGE_BONUS = BUILDER
            .comment("Maximum extra damage multiplier from Hunter Instincts. 1.0 means +100%.")
            .defineInRange("hunterInstinctsMaxDamageBonus", 1.0D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue ARCHITECT_REACH_BONUS = BUILDER
            .comment("Extra block interaction reach in blocks granted by Architect Reach.")
            .defineInRange("architectReachBonus", 3.0D, 0.0D, 64.0D);

    private static final ForgeConfigSpec.DoubleValue DESPERATE_VITALITY_HEALTH_THRESHOLD = BUILDER
            .comment("Health fraction (0-1) below which Desperate Vitality activates.")
            .defineInRange("desperateVitalityHealthThreshold", 0.5D, 0.0D, 1.0D);

    private static final ForgeConfigSpec.DoubleValue DESPERATE_VITALITY_ABSORPTION_HEALTH_PERCENTAGE = BUILDER
            .comment("Temporary absorption health as a fraction of the player's max health.",
                    "0.5 means +50% max health as yellow absorption hearts.")
            .defineInRange("desperateVitalityAbsorptionHealthPercentage", 0.5D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.IntValue DESPERATE_VITALITY_DURATION = BUILDER
            .comment("Duration of the temporary max health bonus in ticks. 200 ticks = 10 seconds.")
            .defineInRange("desperateVitalityDuration", 200, 1, 12000);

    private static final ForgeConfigSpec.IntValue DESPERATE_VITALITY_COOLDOWN = BUILDER
            .comment("Cooldown after the temporary max health bonus ends, in ticks. 6000 ticks = 5 minutes.")
            .defineInRange("desperateVitalityCooldown", 6000, 0, 72000);

    private static final ForgeConfigSpec.DoubleValue WARRIOR_WRATH_MONSTER_RADIUS = BUILDER
            .comment("Radius in blocks around the attacker used to count monsters for Warrior's Wrath.")
            .defineInRange("warriorWrathMonsterRadius", 12.0D, 0.0D, 64.0D);

    private static final ForgeConfigSpec.IntValue WARRIOR_WRATH_MAX_MONSTERS = BUILDER
            .comment("Monster count at which Warrior's Wrath reaches its maximum damage bonus.")
            .defineInRange("warriorWrathMaxMonsters", 12, 1, 64);

    private static final ForgeConfigSpec.DoubleValue WARRIOR_WRATH_MAX_DAMAGE_BONUS = BUILDER
            .comment("Maximum extra damage multiplier from Warrior's Wrath. 1.0 means +100%.")
            .defineInRange("warriorWrathMaxDamageBonus", 1.0D, 0.0D, 10.0D);

    private static final ForgeConfigSpec.DoubleValue SENTINEL_AEGIS_ARMOR_BONUS_PER_HEALTH_PERCENT = BUILDER
            .comment("Armor multiplier added per 1% of missing health. 1.0 means +1% armor for every 1% health lost.")
            .defineInRange("sentinelAegisArmorBonusPerHealthPercent", 1.0D, 0.0D, 10.0D);

    static final ForgeConfigSpec SPEC = BUILDER.pop().build();

    private static boolean debugLogging;
    private static int maxBlessings = 6;

    private static double altarBlessingBoostPerAltar = 1.0D;
    private static double bountifulHarvestChance = 0.5D;
    private static double hunterInstinctsMaxDistance = 64.0D;
    private static double hunterInstinctsMaxDamageBonus = 1.0D;
    private static double architectReachBonus = 3.0D;
    private static double desperateVitalityHealthThreshold = 0.5D;
    private static double desperateVitalityAbsorptionHealthPercentage = 0.5D;
    private static int desperateVitalityDuration = 200;
    private static int desperateVitalityCooldown = 6000;
    private static double warriorWrathMonsterRadius = 12.0D;
    private static int warriorWrathMaxMonsters = 12;
    private static double warriorWrathMaxDamageBonus = 1.0D;
    private static double sentinelAegisArmorBonusPerHealthPercent = 1.0D;

    private Config() {
    }

    public static boolean debugLogging() {
        return debugLogging;
    }

    public static int maxBlessings() {
        return maxBlessings;
    }

    public static double altarBlessingBoostPerAltar() {
        return altarBlessingBoostPerAltar;
    }

    public static double bountifulHarvestChance() {
        return bountifulHarvestChance;
    }

    public static double hunterInstinctsMaxDistance() {
        return hunterInstinctsMaxDistance;
    }

    public static double hunterInstinctsMaxDamageBonus() {
        return hunterInstinctsMaxDamageBonus;
    }

    public static double architectReachBonus() {
        return architectReachBonus;
    }

    public static double desperateVitalityHealthThreshold() {
        return desperateVitalityHealthThreshold;
    }

    public static double desperateVitalityAbsorptionHealthPercentage() {
        return desperateVitalityAbsorptionHealthPercentage;
    }

    public static int desperateVitalityDuration() {
        return desperateVitalityDuration;
    }

    public static int desperateVitalityCooldown() {
        return desperateVitalityCooldown;
    }

    public static double warriorWrathMonsterRadius() {
        return warriorWrathMonsterRadius;
    }

    public static int warriorWrathMaxMonsters() {
        return warriorWrathMaxMonsters;
    }

    public static double warriorWrathMaxDamageBonus() {
        return warriorWrathMaxDamageBonus;
    }

    public static double sentinelAegisArmorBonusPerHealthPercent() {
        return sentinelAegisArmorBonusPerHealthPercent;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Unloading 阶段 ConfigValue 已经不可读，只处理 Loading / Reloading。
        if (event instanceof ModConfigEvent.Unloading) {
            return;
        }
        // MOD 总线的静态处理器会收到所有 mod 的配置事件；别的 mod 的配置先加载时，
        // 本 mod 的 ConfigValue 还不能读（见类注释第 2 条），所以只处理自己的这份配置。
        if (!AncientRemnantsAdd.MOD_ID.equals(event.getConfig().getModId())
                || event.getConfig().getSpec() != SPEC) {
            return;
        }
        debugLogging = DEBUG_LOGGING.get();
        maxBlessings = MAX_BLESSINGS.get();

        altarBlessingBoostPerAltar = ALTAR_BLESSING_BOOST_PER_ALTAR.get();
        bountifulHarvestChance = BOUNTIFUL_HARVEST_CHANCE.get();
        hunterInstinctsMaxDistance = HUNTER_INSTINCTS_MAX_DISTANCE.get();
        hunterInstinctsMaxDamageBonus = HUNTER_INSTINCTS_MAX_DAMAGE_BONUS.get();
        architectReachBonus = ARCHITECT_REACH_BONUS.get();
        desperateVitalityHealthThreshold = DESPERATE_VITALITY_HEALTH_THRESHOLD.get();
        desperateVitalityAbsorptionHealthPercentage = DESPERATE_VITALITY_ABSORPTION_HEALTH_PERCENTAGE.get();
        desperateVitalityDuration = DESPERATE_VITALITY_DURATION.get();
        desperateVitalityCooldown = DESPERATE_VITALITY_COOLDOWN.get();
        warriorWrathMonsterRadius = WARRIOR_WRATH_MONSTER_RADIUS.get();
        warriorWrathMaxMonsters = WARRIOR_WRATH_MAX_MONSTERS.get();
        warriorWrathMaxDamageBonus = WARRIOR_WRATH_MAX_DAMAGE_BONUS.get();
        sentinelAegisArmorBonusPerHealthPercent = SENTINEL_AEGIS_ARMOR_BONUS_PER_HEALTH_PERCENT.get();

        // 建筑师之触的属性修正器是在效果注册时创建的，这里用配置值替换；
        // 注册表尚未就绪时由主类 commonSetup 再调用一次。
        BlessingTuning.applyArchitectReach();
    }
}
