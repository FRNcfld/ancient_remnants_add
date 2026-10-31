package com.frnc.ancient_remnants_add.network;

import com.frnc.ancient_remnants_add.client.waypoint.ClientWaypointStore;
import com.frnc.ancient_remnants_add.waypoint.WaypointMarker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端的完整标记快照。
 *
 * <p>每个维度一组；客户端用它整体重建本 mod 的 Xaero 第三方路径点。完整快照让客户端无需处理
 * 增删增量，也不需要持久化这些路径点。
 */
public record ClientboundMarkerSyncPacket(List<DimMarkers> dimensions) {

    public ClientboundMarkerSyncPacket {
        dimensions = List.copyOf(dimensions);
    }

    public record DimMarkers(ResourceKey<Level> dimension, List<Entry> entries) {
        public DimMarkers {
            entries = List.copyOf(entries);
        }
    }

    public record Entry(WaypointMarker.MarkerType type, BlockPos pos) {
    }

    public static void encode(ClientboundMarkerSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.dimensions().size());
        for (DimMarkers dim : packet.dimensions()) {
            buffer.writeResourceLocation(dim.dimension().location());
            buffer.writeVarInt(dim.entries().size());
            for (Entry entry : dim.entries()) {
                buffer.writeByte(entry.type().ordinal());
                buffer.writeLong(entry.pos().asLong());
            }
        }
    }

    public static ClientboundMarkerSyncPacket decode(FriendlyByteBuf buffer) {
        int dimCount = buffer.readVarInt();
        List<DimMarkers> dimensions = new ArrayList<>(dimCount);
        WaypointMarker.MarkerType[] types = WaypointMarker.MarkerType.values();
        for (int i = 0; i < dimCount; i++) {
            ResourceLocation dimensionId = buffer.readResourceLocation();
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            int markerCount = buffer.readVarInt();
            List<Entry> entries = new ArrayList<>(markerCount);
            for (int j = 0; j < markerCount; j++) {
                int typeId = buffer.readUnsignedByte();
                BlockPos pos = BlockPos.of(buffer.readLong());
                if (typeId >= 0 && typeId < types.length) {
                    entries.add(new Entry(types[typeId], pos));
                }
            }
            dimensions.add(new DimMarkers(dimension, entries));
        }
        return new ClientboundMarkerSyncPacket(dimensions);
    }

    public static void handle(ClientboundMarkerSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        // consumerMainThread 已经切到主线程；服务端不会收到只发往客户端的包。
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientWaypointStore.accept(packet));
    }

}
