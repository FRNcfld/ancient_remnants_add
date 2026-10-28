package com.frnc.ancient_remnants_add.altar;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 把远古之心加入古城箱子的战利品表。 */
@Mod.EventBusSubscriber(modid = AncientRemnantsAdd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AltarAncientCityLoot {

    private static final ResourceLocation ANCIENT_CITY =
            new ResourceLocation("minecraft", "chests/ancient_city");
    private static final ResourceLocation ANCIENT_CITY_ICE_BOX =
            new ResourceLocation("minecraft", "chests/ancient_city_ice_box");

    private static final ResourceLocation EXTENSION =
            new ResourceLocation(AncientRemnantsAdd.MOD_ID, "injections/ancient_city_ancient_heart");

    private AltarAncientCityLoot() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        if (!ANCIENT_CITY.equals(name) && !ANCIENT_CITY_ICE_BOX.equals(name)) {
            return;
        }
        LootPool pool = LootPool.lootPool()
                .add(LootTableReference.lootTableReference(EXTENSION))
                .build();
        event.getTable().addPool(pool);
    }
}
