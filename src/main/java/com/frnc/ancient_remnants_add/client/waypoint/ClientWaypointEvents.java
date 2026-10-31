package com.frnc.ancient_remnants_add.client.waypoint;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 客户端每 tick 尝试把最新快照同步到 Xaero；退出世界时清空快照。
 *
 * <p>Xaero 是软依赖：这里只做存在性判断，真正引用 Xaero 的类在
 * {@link XaeroWaypointApplier} 里。调用点也包一层 try/catch，覆盖旧版 Xaero 在类校验/加载阶段
 * 抛出的 LinkageError，避免整局游戏崩溃。
 */
@Mod.EventBusSubscriber(
        modid = AncientRemnantsAdd.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT
)
public final class ClientWaypointEvents {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static boolean xaeroDisabled;

    private ClientWaypointEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || xaeroDisabled) {
            return;
        }
        if (!ModList.get().isLoaded("xaerominimap")) {
            return;
        }
        try {
            XaeroWaypointApplier.tick();
        } catch (VirtualMachineError error) {
            throw error;
        } catch (Throwable error) {
            xaeroDisabled = true;
            LOGGER.error("[AncientRemnantsAdd] Xaero waypoint integration disabled", error);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientWaypointStore.clear();
        if (xaeroDisabled || !ModList.get().isLoaded("xaerominimap")) {
            return;
        }
        try {
            XaeroWaypointApplier.reset();
        } catch (VirtualMachineError error) {
            throw error;
        } catch (Throwable error) {
            xaeroDisabled = true;
            LOGGER.error("[AncientRemnantsAdd] Xaero waypoint integration disabled", error);
        }
    }
}
