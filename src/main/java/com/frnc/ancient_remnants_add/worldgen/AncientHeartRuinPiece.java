package com.frnc.ancient_remnants_add.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** 把 ancient_heart_ruin 模板放进世界；模板里没有 data marker。 */
public class AncientHeartRuinPiece extends TemplateStructurePiece {

    private static final ResourceLocation TEMPLATE = AncientHeartRuinStructure.ID;

    public AncientHeartRuinPiece(StructureTemplateManager templateManager, BlockPos pos,
                                 StructurePlaceSettings settings) {
        super(WorldgenRegistry.ANCIENT_HEART_RUIN_PIECE.get(), 0, templateManager,
                TEMPLATE, TEMPLATE.toString(), settings, pos);
    }

    public AncientHeartRuinPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(WorldgenRegistry.ANCIENT_HEART_RUIN_PIECE.get(), tag, context.structureTemplateManager(),
                id -> defaultSettings());
    }

    private static StructurePlaceSettings defaultSettings() {
        return new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(true)
                .setKeepLiquids(false);
    }

    @Override
    protected void handleDataMarker(String name, BlockPos pos, ServerLevelAccessor level,
                                    RandomSource random, BoundingBox box) {
        // 模板中没有 data marker。
    }
}
