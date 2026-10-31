package com.frnc.ancient_remnants_add.waypoint;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.frnc.ancient_remnants_add.network.ClientboundMarkerSyncPacket;
import com.frnc.ancient_remnants_add.network.ModNetwork;
import com.mojang.logging.LogUtils;
import dev.obscuria.ancient_remnants.common.Blessing;
import dev.obscuria.ancient_remnants.common.entity.Elderheart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 小地图标记的服务端入口：写入 {@link WaypointMarkerData} 并给对应玩家下发完整快照。
 *
 * <p>标记是「仅本人可见」的，所以每个事件只同步受影响的那名玩家；祭坛熄灭会先从所有玩家
 * 的数据里删掉，在线玩家立即收到新快照，离线玩家下次登录时自然拿到修正后的列表。
 *
 * <p>地图联动是可选功能，入口统一经过 {@link #safely}：Xaero/网络/数据层出问题时只记录日志，
 * 不打断祝福获取、祭坛激活或方块清理等核心流程。
 */
@Mod.EventBusSubscriber(modid = AncientRemnantsAdd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class WaypointMarkerSync {

    private static final Logger LOGGER = LogUtils.getLogger();

    private WaypointMarkerSync() {
    }

    /**
     * 玩家从 Elderheart 获得祝福时调用。只有 16 格内、祝福与本次一致的休眠核心才算数，
     * 这样 {@code /ancient_remnants blessing set} 等非探索途径不会误标遗迹。
     */
    public static void conquerRuinFromElderheart(ServerPlayer player, Holder<Blessing> blessing) {
        safely("conquerRuinFromElderheart", () -> {
            if (blessing == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            Elderheart nearest = null;
            double nearestDistance = Double.MAX_VALUE;
            for (Elderheart elderheart : level.getEntitiesOfClass(
                    Elderheart.class, player.getBoundingBox().inflate(20.0D))) {
                Holder<Blessing> coreBlessing = elderheart.getBlessing();
                if (coreBlessing == null || !sameBlessing(coreBlessing, blessing)) {
                    continue;
                }
                double distance = elderheart.distanceToSqr(player);
                if (distance <= 16.5D * 16.5D && distance < nearestDistance) {
                    nearest = elderheart;
                    nearestDistance = distance;
                }
            }
            if (nearest != null) {
                conquerRuin(player, nearest.blockPosition());
            }
        });
    }

    private static boolean sameBlessing(Holder<Blessing> first, Holder<Blessing> second) {
        if (first.value() == second.value()) {
            return true;
        }
        return first.unwrapKey().isPresent()
                && second.unwrapKey().isPresent()
                && first.unwrapKey().get().equals(second.unwrapKey().get());
    }

    public static void conquerRuin(ServerPlayer player, BlockPos pos) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        WaypointMarker marker = new WaypointMarker(
                WaypointMarker.MarkerType.CONQUERED_RUIN,
                player.serverLevel().dimension(),
                pos);
        if (WaypointMarkerData.get(server).add(player.getUUID(), marker)) {
            sendSnapshot(player);
        }
    }

    public static void altarActivated(ServerPlayer player, ServerLevel level, BlockPos root) {
        safely("altarActivated", () -> {
            MinecraftServer server = level.getServer();
            if (server == null) {
                return;
            }
            WaypointMarker marker = new WaypointMarker(
                    WaypointMarker.MarkerType.ACTIVATED_ALTAR,
                    level.dimension(),
                    root);
            if (WaypointMarkerData.get(server).add(player.getUUID(), marker)) {
                sendSnapshot(player);
            }
        });
    }

    /** 祭坛熄灭：把标记从所有玩家数据里移除，并立即刷新仍在线的受影响玩家。 */
    public static void altarExtinguished(ServerLevel level, BlockPos root) {
        safely("altarExtinguished", () -> {
            MinecraftServer server = level.getServer();
            if (server == null) {
                return;
            }
            List<UUID> affected = WaypointMarkerData.get(server).removeTypeAt(
                    WaypointMarker.MarkerType.ACTIVATED_ALTAR, level.dimension(), root);
            for (UUID playerId : affected) {
                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player != null) {
                    sendSnapshot(player);
                }
            }
        });
    }

    /** 发送该玩家所有维度的完整标记快照（空列表也会发送，客户端据此清空旧标记）。 */
    public static void sendSnapshot(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Set<WaypointMarker> markers = WaypointMarkerData.get(server).forPlayer(player.getUUID());
        Map<ResourceKey<Level>, List<ClientboundMarkerSyncPacket.Entry>> grouped = new LinkedHashMap<>();
        for (WaypointMarker marker : markers) {
            grouped.computeIfAbsent(marker.dimension(), key -> new ArrayList<>())
                    .add(new ClientboundMarkerSyncPacket.Entry(marker.type(), marker.pos()));
        }
        List<ClientboundMarkerSyncPacket.DimMarkers> dimensions = new ArrayList<>(grouped.size());
        for (Map.Entry<ResourceKey<Level>, List<ClientboundMarkerSyncPacket.Entry>> entry
                : grouped.entrySet()) {
            dimensions.add(new ClientboundMarkerSyncPacket.DimMarkers(entry.getKey(), entry.getValue()));
        }
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ClientboundMarkerSyncPacket(dimensions));
    }

    private static void safely(String action, Runnable task) {
        try {
            task.run();
        } catch (Exception error) {
            LOGGER.error("[AncientRemnantsAdd] map marker update failed: {}", action, error);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            safely("loginSnapshot", () -> sendSnapshot(player));
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            safely("dimensionSnapshot", () -> sendSnapshot(player));
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            safely("respawnSnapshot", () -> sendSnapshot(player));
        }
    }
}
