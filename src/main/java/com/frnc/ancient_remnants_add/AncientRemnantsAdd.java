package com.frnc.ancient_remnants_add;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Ancient Remnants Add 主入口。
 *
 * <p>本 mod 是「远古遗迹（Ancient Remnants）」与「机械动力（Create）」双上游的附属模组，
 * 目标是在两者的机制之间搭建联动内容。当前仍是初始化骨架：只有主入口、通用配置与
 * mixin 基础设施，玩法内容尚未落地。
 *
 * <p>后续加内容时的约定（与同仓库其它 1.20.1 工程一致）：
 * <ul>
 *   <li>所有注册内容都挂在 {@link #MOD_ID} 下，用 {@link #id(String)} 生成 {@code ResourceLocation}；</li>
 *   <li>每个功能子包自己持有 {@code DeferredRegister} 并提供一个 {@code register(IEventBus)}，
 *       由本类的构造函数逐个调用；</li>
 *   <li>{@code MinecraftForge.EVENT_BUS.register(this)} 已经在构造函数里接好，
 *       直接加 {@code @SubscribeEvent} 方法即可；</li>
 *   <li>新增 mixin 类时同步登记进 {@code ancient_remnants_add.mixins.json}，否则不会生效。</li>
 * </ul>
 *
 * <p>两个上游的固定信息（写代码时别再硬编码字符串）：
 * <ul>
 *   <li>{@link #CREATE_MOD_ID} —— 机械动力，{@code mods.toml} 里 mandatory=true；</li>
 *   <li>{@link #ANCIENT_REMNANTS_MOD_ID} —— 远古遗迹，{@code mods.toml} 里 mandatory=true。
 *       它自己硬依赖框架 Fragmentum，本 mod 不直接引用 Fragmentum 的类。</li>
 * </ul>
 */
@Mod(AncientRemnantsAdd.MOD_ID)
public class AncientRemnantsAdd {

    /** 本 mod 的命名空间，需与 META-INF/mods.toml 中的 modId 一致。 */
    public static final String MOD_ID = "ancient_remnants_add";

    /** 上游一：机械动力（Create）的 modId。 */
    public static final String CREATE_MOD_ID = "create";

    /** 上游二：远古遗迹（Ancient Remnants）的 modId，取自它自己的 mods.toml。 */
    public static final String ANCIENT_REMNANTS_MOD_ID = "ancient_remnants";

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 快捷创建本 mod 的 ResourceLocation。 */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public AncientRemnantsAdd(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        // 游戏事件总线：后续的事件处理类在这里注册自己，或者直接在本类加 @SubscribeEvent 方法
        MinecraftForge.EVENT_BUS.register(this);

        // 通用配置（config/ancient_remnants_add-common.toml）
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LOGGER.info("[AncientRemnantsAdd] initialized, upstreams: {} + {}", CREATE_MOD_ID, ANCIENT_REMNANTS_MOD_ID);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("[AncientRemnantsAdd] common setup done");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[AncientRemnantsAdd] server starting");
    }
}
