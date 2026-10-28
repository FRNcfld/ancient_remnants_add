package com.frnc.ancient_remnants_add.loot;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootTableReference;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 给 Forge 端补上掠夺者前哨站的残片掉落。
 *
 * <p>远古遗迹 1.5.2 的 jar 里已经有 {@code data/ancient_remnants/loot_tables/extensions/pillager_outpost.json}
 * （50% 概率掉一个随机研究残片），但只有 Fabric 的 {@code FabricLootTableModifiers}
 * 把这张扩展表挂到 {@code minecraft:chests/pillager_outpost}；Forge 端没有任何接入代码，
 * 所以 Forge 玩家实际开前哨站箱子是拿不到的。这里用 LootTableLoadEvent 补上同样的引用。
 */
@Mod.EventBusSubscriber(modid = AncientRemnantsAdd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PillagerOutpostLoot {

    private static final ResourceLocation PILLAGER_OUTPOST =
            new ResourceLocation("minecraft", "chests/pillager_outpost");

    private static final ResourceLocation EXTENSION =
            new ResourceLocation(AncientRemnantsAdd.ANCIENT_REMNANTS_MOD_ID, "extensions/pillager_outpost");

    private PillagerOutpostLoot() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!PILLAGER_OUTPOST.equals(event.getName())) {
            return;
        }

        LootPool extensionPool = LootPool.lootPool()
                .add(LootTableReference.lootTableReference(EXTENSION))
                .build();
        event.getTable().addPool(extensionPool);
    }
}
