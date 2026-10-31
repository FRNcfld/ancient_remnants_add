package com.frnc.ancient_remnants_add.waypoint;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 全服唯一的小地图标记表，按玩家 UUID 保存。
 *
 * <p>放在主世界的 {@code DataStorage} 里，因为它需要跨维度保存同一个玩家的标记。所有访问
 * 都发生在服务端主线程（登录/换维度/祭坛事件与结构探索触发点），不需要额外同步。
 */
public final class WaypointMarkerData extends SavedData {

    private static final String DATA_NAME = "ancient_remnants_add_markers";
    private static final String PLAYERS_TAG = "Players";
    private static final String UUID_TAG = "Uuid";
    private static final String MARKERS_TAG = "Markers";
    private static final String TYPE_TAG = "Type";
    private static final String DIMENSION_TAG = "Dimension";
    private static final String POS_TAG = "Pos";

    private final Map<UUID, Set<WaypointMarker>> markersByPlayer = new HashMap<>();

    private WaypointMarkerData() {
    }

    public static WaypointMarkerData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(WaypointMarkerData::load, WaypointMarkerData::new, DATA_NAME);
    }

    public static WaypointMarkerData load(CompoundTag tag) {
        WaypointMarkerData data = new WaypointMarkerData();
        ListTag players = tag.getList(PLAYERS_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag playerTag = players.getCompound(i);
            if (!playerTag.hasUUID(UUID_TAG)) {
                continue;
            }
            UUID playerId = playerTag.getUUID(UUID_TAG);
            Set<WaypointMarker> markers = new LinkedHashSet<>();
            ListTag markerList = playerTag.getList(MARKERS_TAG, Tag.TAG_COMPOUND);
            for (int j = 0; j < markerList.size(); j++) {
                WaypointMarker marker = loadMarker(markerList.getCompound(j));
                if (marker != null) {
                    markers.add(marker);
                }
            }
            if (!markers.isEmpty()) {
                data.markersByPlayer.put(playerId, markers);
            }
        }
        return data;
    }

    private static WaypointMarker loadMarker(CompoundTag tag) {
        WaypointMarker.MarkerType type = WaypointMarker.MarkerType.byName(tag.getString(TYPE_TAG));
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(DIMENSION_TAG));
        if (type == null || dimensionId == null || !tag.contains(POS_TAG, Tag.TAG_LONG)) {
            return null;
        }
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        return new WaypointMarker(type, dimension, BlockPos.of(tag.getLong(POS_TAG)));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Set<WaypointMarker>> entry : markersByPlayer.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID(UUID_TAG, entry.getKey());
            ListTag markerList = new ListTag();
            for (WaypointMarker marker : entry.getValue()) {
                CompoundTag markerTag = new CompoundTag();
                markerTag.putString(TYPE_TAG, marker.type().name());
                markerTag.putString(DIMENSION_TAG, marker.dimension().location().toString());
                markerTag.putLong(POS_TAG, marker.pos().asLong());
                markerList.add(markerTag);
            }
            playerTag.put(MARKERS_TAG, markerList);
            players.add(playerTag);
        }
        tag.put(PLAYERS_TAG, players);
        return tag;
    }

    /** 添加一条标记；已存在时返回 false。 */
    public boolean add(UUID playerId, WaypointMarker marker) {
        Set<WaypointMarker> markers = markersByPlayer.computeIfAbsent(playerId, key -> new LinkedHashSet<>());
        if (!markers.add(marker)) {
            return false;
        }
        setDirty();
        return true;
    }

    /** 该玩家的全部标记（只读快照）。 */
    public Set<WaypointMarker> forPlayer(UUID playerId) {
        Set<WaypointMarker> markers = markersByPlayer.get(playerId);
        if (markers == null || markers.isEmpty()) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(markers));
    }

    /**
     * 从所有玩家中移除指定类型、维度和位置的标记，返回真正受影响的玩家。
     * 用于祭坛熄灭；离线玩家会在下次登录时拿到修正后的快照。
     */
    public List<UUID> removeTypeAt(WaypointMarker.MarkerType type, ResourceKey<Level> dimension,
                                   BlockPos pos) {
        List<UUID> affected = new ArrayList<>();
        for (Map.Entry<UUID, Set<WaypointMarker>> entry : markersByPlayer.entrySet()) {
            if (entry.getValue().remove(new WaypointMarker(type, dimension, pos))) {
                affected.add(entry.getKey());
            }
        }
        if (!affected.isEmpty()) {
            setDirty();
        }
        return affected;
    }
}
