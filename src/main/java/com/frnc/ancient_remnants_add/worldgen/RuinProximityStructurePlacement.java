package com.frnc.ancient_remnants_add.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

import java.util.Optional;

/**
 * 只在远古遗迹周围 8–32 区块的环带里选一个目标区块。
 *
 * <p>每座遗迹独立掷 5%；不是按区块 5%（那会在环带里生成几十处）。
 * 具体目标由 {@link RuinTargetSelector} 从世界种子确定。
 */
public class RuinProximityStructurePlacement extends StructurePlacement {

    public static final Codec<RuinProximityStructurePlacement> CODEC =
            RecordCodecBuilder.create(instance -> placementCodec(instance)
                    .apply(instance, RuinProximityStructurePlacement::new));

    public RuinProximityStructurePlacement(Vec3i locateOffset,
                                           StructurePlacement.FrequencyReductionMethod frequencyReductionMethod,
                                           float frequency, int salt,
                                           Optional<StructurePlacement.ExclusionZone> exclusionZone) {
        super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone);
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int x, int z) {
        return RuinTargetSelector.isSelected(state, x, z, salt());
    }

    @Override
    public StructurePlacementType<?> type() {
        return WorldgenRegistry.RUIN_PROXIMITY.get();
    }
}
