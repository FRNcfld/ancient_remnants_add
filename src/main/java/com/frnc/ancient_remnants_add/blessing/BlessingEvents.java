package com.frnc.ancient_remnants_add.blessing;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.frnc.ancient_remnants_add.altar.AncientEchoAdvancement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 服务端玩家 tick 时同步多祝福列表。
 *
 * <p>上游只在 Player.tick 尾部 tick 它自己保存的那一个祝福；这里在 tick 结束再遍历
 * 本 mod 的列表，保证同时持有的每个祝福效果都不会因为旧的单祝福逻辑被顶掉。
 */
@Mod.EventBusSubscriber(modid = AncientRemnantsAdd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class BlessingEvents {

    private BlessingEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            MultiBlessings.sync(player);
            DesperateVitalityHandler.tick(player);
            AncientEchoAdvancement.tick(player);
        }
    }
}
