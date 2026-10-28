package com.frnc.ancient_remnants_add.worldgen;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.frnc.ancient_remnants_add.altar.AltarGeometry;
import com.frnc.ancient_remnants_add.altar.AltarSavedData;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

import java.util.Optional;

/**
 * 残缺祭坛结构。
 *
 * <p>模板是一个 32×25×32 的地表遗迹，恰好覆盖一个区块；祭坛位于模板局部
 * (15..16, 3, 15..16)。这里不做随机旋转（自定义 part 枚举不会被 BlockState.rotate 重映射），
 * 海洋/河流附近改用海床高度，保证六种远古遗迹（包括深海里的哨兵巨石）周围都可能生成。
 */
public class AncientHeartRuinStructure extends Structure {

    public static final ResourceLocation ID = AncientRemnantsAdd.id("ancient_heart_ruin");

    public static final Codec<AncientHeartRuinStructure> CODEC =
            simpleCodec(AncientHeartRuinStructure::new);

    private static final int WIDTH = 32;
    private static final int DEPTH = 32;
    private static final int HEIGHT = 25;

    public AncientHeartRuinStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunkPos = context.chunkPos();
        int midX = chunkPos.getMiddleBlockX();
        int midZ = chunkPos.getMiddleBlockZ();

        int surfaceY = context.chunkGenerator().getFirstOccupiedHeight(
                midX, midZ, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState());
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(
                QuartPos.fromBlock(midX),
                QuartPos.fromBlock(surfaceY),
                QuartPos.fromBlock(midZ),
                context.randomState().sampler());

        Heightmap.Types heightmap = biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_RIVER)
                ? Heightmap.Types.OCEAN_FLOOR_WG
                : Heightmap.Types.WORLD_SURFACE_WG;
        int y = lowestCornerY(context, heightmap);
        if (y + HEIGHT > context.heightAccessor().getMaxBuildHeight()) {
            return Optional.empty();
        }

        BlockPos origin = new BlockPos(chunkPos.getMinBlockX(), y, chunkPos.getMinBlockZ());
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE)
                .setIgnoreEntities(true)
                .setKeepLiquids(false);
        BlockPos center = new BlockPos(midX, y, midZ);
        return Optional.of(new Structure.GenerationStub(center, builder ->
                builder.addPiece(new AncientHeartRuinPiece(context.structureTemplateManager(), origin, settings))));
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                           RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, PiecesContainer pieces) {
        if (pieces.isEmpty()) {
            return;
        }
        BoundingBox box = pieces.calculateBoundingBox();
        BlockPos root = AltarGeometry.siteRootFromOrigin(
                new BlockPos(box.minX(), box.minY(), box.minZ()));

        ServerLevel serverLevel = level.getLevel();
        MinecraftServer server = serverLevel.getServer();
        if (server == null) {
            return;
        }
        // afterPlace 运行在区块生成工作线程；SavedData 只能在服务端主线程访问。
        server.execute(() -> AltarSavedData.get(serverLevel).registerSite(root));
    }

    private static int lowestCornerY(Structure.GenerationContext context, Heightmap.Types heightmap) {
        int minX = context.chunkPos().getMinBlockX();
        int minZ = context.chunkPos().getMinBlockZ();
        int a = cornerY(context, heightmap, minX, minZ);
        int b = cornerY(context, heightmap, minX + WIDTH - 1, minZ);
        int c = cornerY(context, heightmap, minX, minZ + DEPTH - 1);
        int d = cornerY(context, heightmap, minX + WIDTH - 1, minZ + DEPTH - 1);
        return Math.min(Math.min(a, b), Math.min(c, d));
    }

    private static int cornerY(Structure.GenerationContext context, Heightmap.Types heightmap, int x, int z) {
        return context.chunkGenerator().getFirstOccupiedHeight(
                x, z, heightmap, context.heightAccessor(), context.randomState());
    }

    @Override
    public StructureType<?> type() {
        return WorldgenRegistry.ANCIENT_HEART_RUIN_STRUCTURE.get();
    }
}
