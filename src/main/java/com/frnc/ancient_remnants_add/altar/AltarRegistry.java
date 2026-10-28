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

/** 远古之心的方块、物品与 BlockEntity 注册。 */
public final class AltarRegistry {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AncientRemnantsAdd.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AncientRemnantsAdd.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AncientRemnantsAdd.MOD_ID);

    public static final RegistryObject<Block> ANCIENT_HEART =
            BLOCKS.register("ancient_heart", AncientHeartBlock::new);

    public static final RegistryObject<Item> ANCIENT_HEART_ITEM =
            ITEMS.register("ancient_heart",
                    () -> new BlockItem(ANCIENT_HEART.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<AncientHeartBlockEntity>> ANCIENT_HEART_BE =
            BLOCK_ENTITIES.register("ancient_heart", () -> BlockEntityType.Builder
                    .of(AncientHeartBlockEntity::new, ANCIENT_HEART.get())
                    .build(null));

    private AltarRegistry() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        ITEMS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
    }
}
