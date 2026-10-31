package com.frnc.ancient_remnants_add.waypoint;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * 一条发给玩家小地图的标记。
 *
 * <p>标记只属于单个玩家（按 UUID 存在 {@link WaypointMarkerData} 里），类型决定客户端使用的
 * 符号与颜色。维度显式记录，客户端据此把路径点放进 Xaero 对应的维度容器。
 */
public record WaypointMarker(MarkerType type, ResourceKey<Level> dimension, BlockPos pos) {

    public WaypointMarker {
        pos = pos.immutable();
    }

    /** 标记类型；名称同时用于 NBT 与客户端路径点 id。 */
    public enum MarkerType {
        /** 已征服的远古遗迹：玩家从 Elderheart 获得了祝福。 */
        CONQUERED_RUIN,
        /** 已激活的残缺祭坛。 */
        ACTIVATED_ALTAR;

        public static MarkerType byName(String name) {
            for (MarkerType type : values()) {
                if (type.name().equals(name)) {
                    return type;
                }
            }
            return null;
        }
    }
}
