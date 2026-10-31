package com.frnc.ancient_remnants_add.client.waypoint;

import com.frnc.ancient_remnants_add.network.ClientboundMarkerSyncPacket;

import java.util.List;

/**
 * 客户端保存的最近一次标记快照。
 *
 * <p>只做缓存，不引用 Xaero；真正把快照变成路径点的是 {@link XaeroWaypointApplier}。
 * 所有方法都在客户端主线程调用（网络包由 {@code consumerMainThread} 派发）。
 */
public final class ClientWaypointStore {

    private static List<ClientboundMarkerSyncPacket.DimMarkers> snapshot = List.of();
    private static long revision;

    private ClientWaypointStore() {
    }

    public static void accept(ClientboundMarkerSyncPacket packet) {
        snapshot = List.copyOf(packet.dimensions());
        revision++;
    }

    public static List<ClientboundMarkerSyncPacket.DimMarkers> snapshot() {
        return snapshot;
    }

    public static long revision() {
        return revision;
    }

    public static void clear() {
        snapshot = List.of();
        revision++;
    }
}
