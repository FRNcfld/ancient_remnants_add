package com.frnc.ancient_remnants_add;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 本 mod 的通用配置（{@code config/ancient_remnants_add-common.toml}）。
 *
 * <p>目前只有一个调试开关，作为后续加可调项的骨架。加新选项时注意三条：
 * <ol>
 *   <li>取值只放在访问器的方法体里（像 {@link #debugLogging()} 这样），不要写成
 *       {@code static final} 字段——配置的加载时机晚于方块/物品的构造；</li>
 *   <li>开发环境里 {@code ConfigValue#get()} 在配置未加载时会抛 {@code IllegalStateException}
 *       （生产环境才退回默认值），所以不要在加载期调用；</li>
 *   <li>{@code .comment()} 里不要出现 ASCII 双引号，改完同步更新 README 里的配置表。</li>
 * </ol>
 */
@Mod.EventBusSubscriber(modid = AncientRemnantsAdd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config {

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue DEBUG_LOGGING = BUILDER
            .comment("Whether to print this mod's debug logs to the game log.")
            .define("debugLogging", false);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    private static boolean debugLogging;

    private Config() {
    }

    public static boolean debugLogging() {
        return debugLogging;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        debugLogging = DEBUG_LOGGING.get();
    }
}
