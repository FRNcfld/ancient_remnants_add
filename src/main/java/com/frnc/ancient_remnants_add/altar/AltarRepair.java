package com.frnc.ancient_remnants_add.altar;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 激活时把祭坛结构「修复」为原始模板，并清除结构范围内的无关方块/非生物实体。
 *
 * <p>以站点 ROOT 推算模板 origin，读取模组自带的 {@code ancient_heart_ruin.nbt}：
 * <ul>
 *   <li>模板中非空气的位置：恢复为模板方块（修复被拆/被替换的结构）；</li>
 *   <li>模板中空气的位置：清成空气（清掉玩家方块、地形、液体等无关方块）；</li>
 *   <li>中央 2×2 远古之心所在四格跳过，不会被清掉。</li>
 * </ul>
 *
 * <p>实体清理见 {@link #clearEntities(ServerLevel, BlockPos)}：箭矢、掉落物、末影珍珠、
 * 经验球等非生物实体会被移除；玩家和生物保留。
 *
 * <p>只在服务端激活仪式里调用；模板只在首次使用时解析并缓存。
 */
public final class AltarRepair {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TEMPLATE_PATH =
            "/data/ancient_remnants_add/structures/ancient_heart_ruin.nbt";
    private static final Object LOCK = new Object();

    private static Map<BlockPos, BlockState> cachedTemplate;
    private static Vec3i cachedSize = Vec3i.ZERO;

    private AltarRepair() {
    }

    public static void repair(ServerLevel level, BlockPos root) {
        Map<BlockPos, BlockState> template = template(level);
        if (template.isEmpty()
                || cachedSize.getX() <= 0 || cachedSize.getY() <= 0 || cachedSize.getZ() <= 0) {
            return;
        }

        BlockPos origin = root.offset(
                -AltarGeometry.HEART_X,
                -AltarGeometry.HEART_Y,
                -AltarGeometry.HEART_Z);
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int x = 0; x < cachedSize.getX(); x++) {
            for (int y = 0; y < cachedSize.getY(); y++) {
                for (int z = 0; z < cachedSize.getZ(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    if (isHeartCell(local)) {
                        continue;
                    }
                    BlockState target = template.getOrDefault(local, air);
                    BlockPos world = origin.offset(local);
                    if (level.getBlockState(world) != target) {
                        // flags=3：同步客户端并更新邻居/光照，修复后的结构立即可见。
                        level.setBlock(world, target, 3);
                    }
                }
            }
        }
    }

    /**
     * 清除结构范围内的「无关实体」：箭矢、掉落物、飞行中的末影珍珠、经验球、
     * 掉落沙、TNT、船/矿车等非生物实体。
     *
     * <p>玩家和生物（村民、宠物等）保留；献祭仍可以在激活后把生物带进祭坛完成。
     * 如果以后要连生物一起清，把谓词改成 {@code !(entity instanceof Player)} 即可。
     */
    public static void clearEntities(ServerLevel level, BlockPos root) {
        // 确保模板已加载并拿到尺寸。
        template(level);
        if (cachedSize.getX() <= 0 || cachedSize.getY() <= 0 || cachedSize.getZ() <= 0) {
            return;
        }
        BlockPos origin = root.offset(
                -AltarGeometry.HEART_X,
                -AltarGeometry.HEART_Y,
                -AltarGeometry.HEART_Z);
        AABB area = new AABB(
                origin.getX(), origin.getY(), origin.getZ(),
                origin.getX() + cachedSize.getX(),
                origin.getY() + cachedSize.getY(),
                origin.getZ() + cachedSize.getZ());
        for (Entity entity : level.getEntitiesOfClass(Entity.class, area, e -> !(e instanceof LivingEntity))) {
            entity.discard();
        }
    }

    private static boolean isHeartCell(BlockPos local) {
        return local.getY() == AltarGeometry.HEART_Y
                && (local.getX() == AltarGeometry.HEART_X || local.getX() == AltarGeometry.HEART_X + 1)
                && (local.getZ() == AltarGeometry.HEART_Z || local.getZ() == AltarGeometry.HEART_Z + 1);
    }

    private static Map<BlockPos, BlockState> template(ServerLevel level) {
        synchronized (LOCK) {
            if (cachedTemplate != null) {
                return cachedTemplate;
            }
            cachedTemplate = Map.of();
            cachedSize = Vec3i.ZERO;

            try (InputStream input = AltarRepair.class.getResourceAsStream(TEMPLATE_PATH)) {
                if (input == null) {
                    LOGGER.warn("[AncientRemnantsAdd] Missing altar template {}", TEMPLATE_PATH);
                    return cachedTemplate;
                }

                CompoundTag root = NbtIo.readCompressed(input);
                ListTag paletteTag = root.getList("palette", Tag.TAG_COMPOUND);
                HolderLookup<Block> blockLookup = level.holderLookup(Registries.BLOCK);
                BlockState[] palette = new BlockState[paletteTag.size()];
                for (int i = 0; i < paletteTag.size(); i++) {
                    palette[i] = NbtUtils.readBlockState(blockLookup, paletteTag.getCompound(i));
                }

                Map<BlockPos, BlockState> template = new HashMap<>();
                ListTag blocksTag = root.getList("blocks", Tag.TAG_COMPOUND);
                for (int i = 0; i < blocksTag.size(); i++) {
                    CompoundTag entry = blocksTag.getCompound(i);
                    ListTag posTag = entry.getList("pos", Tag.TAG_INT);
                    BlockPos local = new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2));
                    int state = entry.getInt("state");
                    if (state < 0 || state >= palette.length) {
                        continue;
                    }
                    BlockState blockState = palette[state];
                    if (!blockState.isAir()) {
                        template.put(local, blockState);
                    }
                }

                ListTag sizeTag = root.getList("size", Tag.TAG_INT);
                cachedSize = new Vec3i(sizeTag.getInt(0), sizeTag.getInt(1), sizeTag.getInt(2));
                cachedTemplate = template;
                LOGGER.info("[AncientRemnantsAdd] Loaded altar repair template: {} blocks, size {}",
                        template.size(), cachedSize);
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("[AncientRemnantsAdd] Failed to load altar repair template", exception);
            }
            return cachedTemplate;
        }
    }
}
