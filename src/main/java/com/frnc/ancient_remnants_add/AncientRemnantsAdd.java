package com.frnc.ancient_remnants_add;

import com.frnc.ancient_remnants_add.altar.AltarRegistry;
import com.frnc.ancient_remnants_add.blessing.BlessingTuning;
import com.frnc.ancient_remnants_add.network.ModNetwork;
import com.frnc.ancient_remnants_add.worldgen.WorldgenRegistry;
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
 * <p>本 mod 是远古遗迹（Ancient Remnants）的附属模组，并把机械动力（Create）、
 * Explorer's Compass 与 Xaero 小地图作为可选联动。当前已实现：
 * <ul>
 *   <li>多祝福：玩家可同时持有最多 {@code maxBlessings}（默认 6）个祝福，满额时替换最早获得的；</li>
 *   <li>祝福数值：六个祝福的伤害/距离/概率/阈值等数值都可在 {@code [blessings]} 配置段调整；</li>
 *   <li>掠夺者前哨站残片：补上 Forge 端缺失的战利品表接入，与 Fabric 行为一致；</li>
 *   <li>远古之心：2×2×1 多方块，古城战利品、集齐典籍后获得「远古回响」挑战进度、
 *       放置完整典籍后把丰收祝福扩展到整个维度；</li>
 *   <li>残缺祭坛：远古遗迹周围 8–32 区块、每座遗迹 5% 概率生成；</li>
 *   <li>Create 联动：祭坛激活的维度内，Create 概率配方产出的 chance ×1.5（封顶 100%）；</li>
 *   <li>Explorer's Compass 联动：结构指南针搜索时跳过已激活的残缺祭坛；</li>
 *   <li>Xaero 小地图联动：按玩家记录已征服的遗迹与已激活的祭坛并显示为路径点。</li>
 * </ul>
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
 * <p>两个上游/联动的固定信息（写代码时别再硬编码字符串）：
 * <ul>
 *   <li>{@link #CREATE_MOD_ID} —— 机械动力，可选联动，{@code mods.toml} 里 mandatory=false；</li>
 *   <li>{@link #ANCIENT_REMNANTS_MOD_ID} —— 远古遗迹，硬依赖，{@code mods.toml} 里 mandatory=true。
 *       它自己硬依赖框架 Fragmentum。</li>
 * </ul>
 */
@Mod(AncientRemnantsAdd.MOD_ID)
public class AncientRemnantsAdd {

    /** 本 mod 的命名空间，需与 META-INF/mods.toml 中的 modId 一致。 */
    public static final String MOD_ID = "ancient_remnants_add";

    /** 可选联动：机械动力（Create）的 modId。 */
    public static final String CREATE_MOD_ID = "create";

    /** 硬依赖：远古遗迹（Ancient Remnants）的 modId，取自它自己的 mods.toml。 */
    public static final String ANCIENT_REMNANTS_MOD_ID = "ancient_remnants";

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 快捷创建本 mod 的 ResourceLocation。 */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    /** 只在配置的 {@code debugLogging} 打开时输出；前缀统一，便于从日志里单独过滤。 */
    public static void debug(String message, Object... args) {
        if (Config.debugLogging()) {
            LOGGER.info("[AncientRemnantsAdd] " + message, args);
        }
    }

    public AncientRemnantsAdd(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        // 远古之心的方块 / 物品 / BlockEntity
        AltarRegistry.register(modEventBus);

        // 残缺祭坛的结构类型 / 放置方式 / 结构片段
        WorldgenRegistry.register(modEventBus);

        // 游戏事件总线：后续的事件处理类在这里注册自己，或者直接在本类加 @SubscribeEvent 方法
        MinecraftForge.EVENT_BUS.register(this);

        // 通用配置（config/ancient_remnants_add-common.toml）
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LOGGER.info("[AncientRemnantsAdd] initialized, required: {}, optional integration: {}",
                ANCIENT_REMNANTS_MOD_ID, CREATE_MOD_ID);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // 小地图标记的服务端 → 客户端同步通道。
            ModNetwork.register();
            // 建筑师之触的属性修正器要在注册表就绪后用配置值刷新。
            BlessingTuning.applyArchitectReach();
        });
        LOGGER.info("[AncientRemnantsAdd] common setup done");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[AncientRemnantsAdd] server starting");
    }
}
