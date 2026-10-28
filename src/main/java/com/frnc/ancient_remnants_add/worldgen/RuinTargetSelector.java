package com.frnc.ancient_remnants_add.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.List;

/**
 * 计算“试炼遗迹周围的残缺祭坛”目标区块。
 *
 * <p>六种远古遗迹的 structure_set 都是 {@link RandomSpreadStructurePlacement}。对候选区块，
 * 枚举所有可能落在 32 区块内的遗迹 home chunk，按 home 独立掷 5%，再确定性地在 8–32
 * 的方形环上选一个目标区块。目标只取决于世界种子与 home，因此完全可复现。
 */
public final class RuinTargetSelector {

    private static final List<ResourceLocation> RUIN_SET_IDS = List.of(
            new ResourceLocation("ancient_remnants", "hunter_monolith"),
            new ResourceLocation("ancient_remnants", "warrior_monolith"),
            new ResourceLocation("ancient_remnants", "sentinel_monolith"),
            new ResourceLocation("ancient_remnants", "cherry_monument"),
            new ResourceLocation("ancient_remnants", "oakwood_monument"),
            new ResourceLocation("ancient_remnants", "coniferous_monument")
    );

    private static final int RING_MIN = 8;
    private static final int RING_MAX = 32;
    private static final float CHANCE = 0.05F;

    private RuinTargetSelector() {
    }

    public static boolean isSelected(ChunkGeneratorStructureState state, int x, int z, int placementSalt) {
        long seed = state.getLevelSeed();
        for (Holder<StructureSet> holder : state.possibleStructureSets()) {
            ResourceLocation id = holder.unwrapKey().map(key -> key.location()).orElse(null);
            int index = id == null ? -1 : RUIN_SET_IDS.indexOf(id);
            if (index < 0) {
                continue;
            }
            StructurePlacement placement = holder.value().placement();
            if (!(placement instanceof RandomSpreadStructurePlacement spread)) {
                continue;
            }
            if (hasTarget(state, seed, spread, x, z, placementSalt, index)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTarget(ChunkGeneratorStructureState state, long seed,
                                     RandomSpreadStructurePlacement spread, int x, int z,
                                     int placementSalt, int index) {
        int spacing = spread.spacing();
        int spreadWidth = spacing - spread.separation();
        int maxOffset = spreadWidth - 1;
        int minCellX = Math.floorDiv(x - RING_MAX - maxOffset, spacing);
        int maxCellX = Math.floorDiv(x + RING_MAX, spacing);
        int minCellZ = Math.floorDiv(z - RING_MAX - maxOffset, spacing);
        int maxCellZ = Math.floorDiv(z + RING_MAX, spacing);

        for (int cellX = minCellX; cellX <= maxCellX; cellX++) {
            for (int cellZ = minCellZ; cellZ <= maxCellZ; cellZ++) {
                ChunkPos home = spread.getPotentialStructureChunk(seed, cellX * spacing, cellZ * spacing);
                int distance = Math.max(Math.abs(home.x - x), Math.abs(home.z - z));
                if (distance < RING_MIN || distance > RING_MAX) {
                    continue;
                }
                if (!spread.isStructureChunk(state, home.x, home.z)) {
                    continue;
                }
                if (targetsHome(seed, home, x, z, placementSalt, index)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean targetsHome(long seed, ChunkPos home, int x, int z,
                                       int placementSalt, int index) {
        WorldgenRandom roll = new WorldgenRandom(new LegacyRandomSource(0L));
        roll.setLargeFeatureWithSalt(seed, home.x, home.z, placementSalt + index * 0x9E3779B9);
        if (roll.nextFloat() >= CHANCE) {
            return false;
        }

        WorldgenRandom pick = new WorldgenRandom(new LegacyRandomSource(0L));
        pick.setLargeFeatureWithSalt(seed, home.x, home.z, placementSalt * 31 + index);
        int distance = RING_MIN + pick.nextInt(RING_MAX - RING_MIN + 1);
        int side = pick.nextInt(4);
        int along = pick.nextInt(2 * distance + 1) - distance;

        int offsetX;
        int offsetZ;
        switch (side) {
            case 0 -> {
                offsetX = along;
                offsetZ = -distance;
            }
            case 1 -> {
                offsetX = distance;
                offsetZ = along;
            }
            case 2 -> {
                offsetX = along;
                offsetZ = distance;
            }
            default -> {
                offsetX = -distance;
                offsetZ = along;
            }
        }
        return home.x + offsetX == x && home.z + offsetZ == z;
    }
}
