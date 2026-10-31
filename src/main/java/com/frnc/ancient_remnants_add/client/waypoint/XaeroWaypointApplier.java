package com.frnc.ancient_remnants_add.client.waypoint;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.frnc.ancient_remnants_add.network.ClientboundMarkerSyncPacket;
import com.frnc.ancient_remnants_add.waypoint.WaypointMarker;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.WaypointPurpose;
import xaero.hud.minimap.waypoint.thirdparty.ThirdPartyWaypoints;
import xaero.hud.minimap.world.container.MinimapWorldContainer;
import xaero.hud.minimap.world.container.MinimapWorldRootContainer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 把服务端快照重建为 Xaero 的第三方路径点（小地图与世界地图共用同一份数据）。
 *
 * <p>这是本 mod 唯一直接引用 Xaero 类的类，只会在客户端、且 {@code xaerominimap} 已加载时
 * 被调用。Xaero 是软依赖，不同版本内部 API 可能变化：所有异常都收敛为「本会话禁用联动」，
 * 不允许影响游戏本身。
 *
 * <p>第三方路径点不会被 Xaero 持久化，所以每次登录/换维度/服务端数据变化都会由
 * {@link ClientWaypointStore} 提供新快照，这里整体重建。维度显式取自标记本身，不依赖
 * 「当前世界」，避免换维度时把标记写错维度。
 */
public final class XaeroWaypointApplier {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ResourceLocation ORIGIN = AncientRemnantsAdd.id("markers");
    private static final String SYMBOL_RUIN = "◈";
    private static final String SYMBOL_ALTAR = "⛩";
    private static final String NAME_RUIN = "waypoint.ancient_remnants_add.conquered_ruin";
    private static final String NAME_ALTAR = "waypoint.ancient_remnants_add.activated_altar";

    /** 上一次重建时涉及过的维度；用于清掉快照里已经消失的维度。 */
    private static final Set<ResourceKey<Level>> appliedDimensions = new HashSet<>();

    private static MinimapSession lastSession;
    private static MinimapWorldRootContainer lastRoot;
    private static long lastRevision = -1L;
    private static int ticksSinceApply;
    private static boolean disabled;

    private XaeroWaypointApplier() {
    }

    public static void tick() {
        if (disabled) {
            return;
        }
        try {
            MinimapSession session = (MinimapSession) BuiltInHudModules.MINIMAP.getCurrentSession();
            if (session == null) {
                return;
            }
            MinimapWorldRootContainer root = session.getWorldManager().getAutoRootContainer();
            if (root == null) {
                return;
            }

            long revision = ClientWaypointStore.revision();
            boolean connectionChanged = session != lastSession || root != lastRoot;
            boolean dataChanged = revision != lastRevision;
            ticksSinceApply++;
            boolean reconcile = ticksSinceApply >= 100;
            if (!connectionChanged && !dataChanged && !reconcile) {
                return;
            }

            if (connectionChanged) {
                // 旧世界的容器已经作废，别在新会话里为旧维度创建空容器。
                appliedDimensions.clear();
            }
            apply(session, root, ClientWaypointStore.snapshot());
            lastSession = session;
            lastRoot = root;
            lastRevision = revision;
            ticksSinceApply = 0;
        } catch (VirtualMachineError error) {
            throw error;
        } catch (Throwable error) {
            disabled = true;
            LOGGER.error("[AncientRemnantsAdd] Xaero waypoint integration disabled", error);
        }
    }

    private static void apply(MinimapSession session, MinimapWorldRootContainer root,
                              List<ClientboundMarkerSyncPacket.DimMarkers> snapshot) {
        Set<ResourceKey<Level>> newDimensions = new HashSet<>();
        for (ClientboundMarkerSyncPacket.DimMarkers dim : snapshot) {
            newDimensions.add(dim.dimension());
        }

        // 先清掉本次涉及的所有维度（包括上一轮有、这一轮已空的维度），再整体重建。
        Set<ResourceKey<Level>> toClear = new HashSet<>(appliedDimensions);
        toClear.addAll(newDimensions);
        for (ResourceKey<Level> dimension : toClear) {
            MinimapWorldContainer container = findContainer(session, root, dimension);
            if (container != null) {
                container.getThirdPartyWaypointManager().get(ORIGIN).clear();
            }
        }

        for (ClientboundMarkerSyncPacket.DimMarkers dim : snapshot) {
            if (dim.entries().isEmpty()) {
                continue;
            }
            MinimapWorldContainer container = findContainer(session, root, dim.dimension());
            if (container == null) {
                continue;
            }
            ThirdPartyWaypoints waypoints = container.getThirdPartyWaypointManager().get(ORIGIN);
            for (ClientboundMarkerSyncPacket.Entry entry : dim.entries()) {
                Waypoint waypoint = new Waypoint(
                        entry.pos().getX(),
                        entry.pos().getY(),
                        entry.pos().getZ(),
                        Component.translatable(nameKey(entry.type())).getString(),
                        symbol(entry.type()),
                        color(entry.type()),
                        WaypointPurpose.NORMAL,
                        false,  // temporary：不要被当成临时路径点
                        true);  // yIncluded：显示高度
                waypoints.add(entry.type().name() + ":" + entry.pos().asLong(), waypoint);
            }
        }

        appliedDimensions.clear();
        appliedDimensions.addAll(newDimensions);
    }

    private static MinimapWorldContainer findContainer(MinimapSession session, MinimapWorldRootContainer root,
                                                       ResourceKey<Level> dimension) {
        String directory = session.getDimensionHelper().getDimensionDirectoryName(dimension);
        if (directory == null) {
            return null;
        }
        return root.addSubContainer(root.getPath().resolve(directory));
    }

    private static String nameKey(WaypointMarker.MarkerType type) {
        return type == WaypointMarker.MarkerType.CONQUERED_RUIN ? NAME_RUIN : NAME_ALTAR;
    }

    private static String symbol(WaypointMarker.MarkerType type) {
        return type == WaypointMarker.MarkerType.CONQUERED_RUIN ? SYMBOL_RUIN : SYMBOL_ALTAR;
    }

    private static WaypointColor color(WaypointMarker.MarkerType type) {
        return type == WaypointMarker.MarkerType.CONQUERED_RUIN
                ? WaypointColor.DARK_PURPLE
                : WaypointColor.GOLD;
    }

    /** 退出世界时调用；下一次进入世界会从头重建。 */
    public static void reset() {
        appliedDimensions.clear();
        lastSession = null;
        lastRoot = null;
        lastRevision = -1L;
        ticksSinceApply = 0;
        disabled = false;
    }
}
