package com.frnc.ancient_remnants_add.altar;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.frnc.ancient_remnants_add.blessing.BlessingIds;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.obscuria.ancient_remnants.common.Blessing;
import dev.obscuria.ancient_remnants.registry.AncientRemnantsRegistries;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 祭坛祝福加成数据文件。
 *
 * <p>路径 {@code data/ancient_remnants_add/altar_boost.json}，结构：
 * <pre>
 * {
 *   "blessings": [{"blessing": "ancient_remnants:hunter_instincts", "max_stacks": 9}, ...],
 *   "spread_dimensions": ["minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"]
 * }
 * </pre>
 *
 * <p>祝福条目决定哪些祝福被加成、以及每种的叠加层数上限（按激活祭坛数计）；
 * {@code spread_dimensions} 决定 9+ 祭坛时加成还能扩散到哪些维度。
 * 数据包可以覆盖本文件；文件缺失使用默认值，解析失败保留上一个快照。
 */
@Mod.EventBusSubscriber(modid = AncientRemnantsAdd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AltarBoostData implements PreparableReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final ResourceLocation FILE =
            new ResourceLocation(AncientRemnantsAdd.MOD_ID, "altar_boost.json");
    private static final int DEFAULT_MAX_STACKS = 9;

    private static final Snapshot DEFAULT = defaultSnapshot();
    private static volatile Snapshot current = DEFAULT;

    private final RegistryAccess registryAccess;

    private AltarBoostData(RegistryAccess registryAccess) {
        this.registryAccess = registryAccess;
    }

    public static Snapshot current() {
        return current;
    }

    public static int maxStacks(ResourceLocation blessingId) {
        return current.caps().getOrDefault(blessingId, 0);
    }

    public static boolean isSpreadDimension(ResourceLocation dimensionId) {
        return current.spreadDimensions().contains(dimensionId);
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new AltarBoostData(event.getRegistryAccess()));
    }

    @Override
    public CompletableFuture<Void> reload(PreparableReloadListener.PreparationBarrier barrier,
                                          ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.supplyAsync(() -> load(resourceManager), backgroundExecutor)
                .thenCompose(snapshot -> barrier.wait(snapshot))
                .thenAcceptAsync(snapshot -> {
                    if (snapshot != null) {
                        current = snapshot;
                        LOGGER.info("[AncientRemnantsAdd] Loaded altar boost data: {} blessings, {} spread dimensions",
                                snapshot.caps().size(), snapshot.spreadDimensions().size());
                    }
                }, gameExecutor);
    }

    private Snapshot load(ResourceManager resourceManager) {
        List<Resource> stack = resourceManager.getResourceStack(FILE);
        if (stack.isEmpty()) {
            // 文件缺失：恢复默认，避免残留旧数据。
            return DEFAULT;
        }
        Resource resource = stack.get(stack.size() - 1);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json == null) {
                LOGGER.warn("[AncientRemnantsAdd] Empty altar boost data, keeping previous snapshot");
                return null;
            }
            return parse(json);
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("[AncientRemnantsAdd] Failed to parse altar boost data, keeping previous snapshot", exception);
            return null;
        }
    }

    private Snapshot parse(JsonObject json) {
        Registry<Blessing> blessingRegistry = blessingRegistry();
        Registry<Level> dimensionRegistry = dimensionRegistry();

        Map<ResourceLocation, Integer> caps = new HashMap<>();
        if (json.has("blessings") && json.get("blessings").isJsonArray()) {
            JsonArray blessings = json.getAsJsonArray("blessings");
            for (JsonElement element : blessings) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject entry = element.getAsJsonObject();
                if (!entry.has("blessing") || !entry.has("max_stacks")) {
                    LOGGER.warn("[AncientRemnantsAdd] altar_boost blessing entry missing blessing/max_stacks: {}",
                            entry);
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(entry.get("blessing").getAsString());
                int maxStacks;
                try {
                    maxStacks = entry.get("max_stacks").getAsInt();
                } catch (RuntimeException exception) {
                    LOGGER.warn("[AncientRemnantsAdd] Invalid max_stacks for {}", entry);
                    continue;
                }
                if (id == null || maxStacks < 0 || maxStacks > 1024) {
                    LOGGER.warn("[AncientRemnantsAdd] Invalid altar_boost entry: {}", entry);
                    continue;
                }
                if (blessingRegistry != null && !blessingRegistry.containsKey(id)) {
                    LOGGER.warn("[AncientRemnantsAdd] Unknown blessing in altar_boost: {}", id);
                    continue;
                }
                if (caps.put(id, maxStacks) != null) {
                    LOGGER.warn("[AncientRemnantsAdd] Duplicate blessing in altar_boost, using last entry: {}", id);
                }
            }
        }

        Set<ResourceLocation> dimensions = new HashSet<>();
        if (json.has("spread_dimensions") && json.get("spread_dimensions").isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray("spread_dimensions")) {
                if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
                if (id == null) {
                    LOGGER.warn("[AncientRemnantsAdd] Invalid spread dimension: {}", element);
                    continue;
                }
                if (dimensionRegistry != null && !dimensionRegistry.containsKey(id)) {
                    LOGGER.warn("[AncientRemnantsAdd] Unknown spread dimension: {}", id);
                    continue;
                }
                dimensions.add(id);
            }
        }

        return new Snapshot(caps, dimensions);
    }

    private Registry<Blessing> blessingRegistry() {
        try {
            return registryAccess.registryOrThrow(AncientRemnantsRegistries.Keys.INSTANCE.getBLESSING());
        } catch (RuntimeException exception) {
            LOGGER.warn("[AncientRemnantsAdd] Blessing registry unavailable while reloading altar boost data",
                    exception);
            return null;
        }
    }

    private Registry<Level> dimensionRegistry() {
        try {
            return registryAccess.registryOrThrow(Registries.DIMENSION);
        } catch (RuntimeException exception) {
            LOGGER.warn("[AncientRemnantsAdd] Dimension registry unavailable while reloading altar boost data",
                    exception);
            return null;
        }
    }

    private static Snapshot defaultSnapshot() {
        Map<ResourceLocation, Integer> caps = new HashMap<>();
        for (ResourceLocation id : BlessingIds.ALL) {
            caps.put(id, DEFAULT_MAX_STACKS);
        }
        Set<ResourceLocation> dimensions = Set.of(
                new ResourceLocation("minecraft", "overworld"),
                new ResourceLocation("minecraft", "the_nether"),
                new ResourceLocation("minecraft", "the_end")
        );
        return new Snapshot(caps, dimensions);
    }

    /** 不可变快照；由数据重载在服务器线程替换。 */
    public record Snapshot(Map<ResourceLocation, Integer> caps, Set<ResourceLocation> spreadDimensions) {

        public Snapshot {
            caps = Map.copyOf(caps);
            spreadDimensions = Set.copyOf(spreadDimensions);
        }
    }
}
