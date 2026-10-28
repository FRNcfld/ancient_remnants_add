package com.frnc.ancient_remnants_add.client;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.frnc.ancient_remnants_add.altar.AltarRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = AncientRemnantsAdd.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class AltarClientEvents {

    private AltarClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AltarRegistry.MONOLITH_ALTAR_BE.get(), MonolithAltarRenderer::new);
    }
}
