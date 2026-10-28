package com.frnc.ancient_remnants_add.worldgen;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** 残缺祭坛的世界生成：结构、结构放置方式与结构片段。 */
public final class WorldgenRegistry {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, AncientRemnantsAdd.MOD_ID);
    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, AncientRemnantsAdd.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, AncientRemnantsAdd.MOD_ID);

    public static final RegistryObject<StructureType<?>> ANCIENT_HEART_RUIN_STRUCTURE =
            STRUCTURE_TYPES.register("ancient_heart_ruin", () -> new StructureType<AncientHeartRuinStructure>() {
                @Override
                public Codec<AncientHeartRuinStructure> codec() {
                    return AncientHeartRuinStructure.CODEC;
                }
            });

    public static final RegistryObject<StructurePlacementType<?>> RUIN_PROXIMITY =
            PLACEMENT_TYPES.register("ruin_proximity",
                    () -> new StructurePlacementType<RuinProximityStructurePlacement>() {
                        @Override
                        public Codec<RuinProximityStructurePlacement> codec() {
                            return RuinProximityStructurePlacement.CODEC;
                        }
                    });

    public static final RegistryObject<StructurePieceType> ANCIENT_HEART_RUIN_PIECE =
            PIECE_TYPES.register("ancient_heart_ruin",
                    () -> (StructurePieceType) (context, tag) -> new AncientHeartRuinPiece(context, tag));

    private WorldgenRegistry() {
    }

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
        PLACEMENT_TYPES.register(eventBus);
        PIECE_TYPES.register(eventBus);
    }
}
