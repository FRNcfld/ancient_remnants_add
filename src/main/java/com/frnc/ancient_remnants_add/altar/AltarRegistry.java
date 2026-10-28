package com.frnc.ancient_remnants_add.altar;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** 巨石祭坛的方块、物品与 BlockEntity 注册。 */
public final class AltarRegistry {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AncientRemnantsAdd.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AncientRemnantsAdd.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AncientRemnantsAdd.MOD_ID);

    public static final RegistryObject<Block> MONOLITH_ALTAR =
            BLOCKS.register("monolith_altar", MonolithAltarBlock::new);

    public static final RegistryObject<Item> MONOLITH_ALTAR_ITEM =
            ITEMS.register("monolith_altar",
                    () -> new BlockItem(MONOLITH_ALTAR.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<MonolithAltarBlockEntity>> MONOLITH_ALTAR_BE =
            BLOCK_ENTITIES.register("monolith_altar", () -> BlockEntityType.Builder
                    .of(MonolithAltarBlockEntity::new, MONOLITH_ALTAR.get())
                    .build(null));

    private AltarRegistry() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
    }
}
