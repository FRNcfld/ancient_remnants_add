package com.frnc.ancient_remnants_add.altar;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 把巨石祭坛放进创造模式的「工具与实用物品」标签（与典籍、残片一致，搜索也能搜到）。 */
@Mod.EventBusSubscriber(modid = AncientRemnantsAdd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AltarCreativeTab {

    private AltarCreativeTab() {
    }

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES)) {
            event.accept(AltarRegistry.MONOLITH_ALTAR_ITEM.get());
        }
    }
}
