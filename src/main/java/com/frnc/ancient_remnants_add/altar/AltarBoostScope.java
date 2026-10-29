package com.frnc.ancient_remnants_add.altar;

import com.frnc.ancient_remnants_add.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 祭坛祝福加成的范围与叠层计算。
 *
 * <ul>
 *   <li>&lt;3 座：只有玩家在任一激活祭坛的 32×25×32 范围内才计数；</li>
 *   <li>3–8 座：该维度所有玩家；</li>
 *   <li>9+ 座：源维度 + 数据文件列出的扩散维度；多个 9+ 源取最大源计数。</li>
 * </ul>
 *
 * <p>倍率 = 1 + 配置的每座加成 × min(有效祭坛数, 每祝福叠加上限)。
 * 只在服务端主线程返回非 1 的值；客户端/非玩家实体一律 1.0。
 */
public final class AltarBoostScope {

    public static final int DIMENSION_WIDE_MIN = 3;
    public static final int WORLD_WIDE_MIN = 9;

    private AltarBoostScope() {
    }

    /** 指定祝福的祭坛倍率；1.0 表示无加成。 */
    public static double multiplierFor(LivingEntity entity, ResourceLocation blessingId) {
        if (!(entity instanceof ServerPlayer player)) {
            return 1.0D;
        }
        ServerLevel level = player.serverLevel();
        MinecraftServer server = level.getServer();
        if (server == null || !server.isSameThread()) {
            return 1.0D;
        }
        int cap = AltarBoostData.maxStacks(blessingId);
        if (cap <= 0) {
            return 1.0D;
        }
        int count = effectiveAltarCount(player, level, server);
        if (count <= 0) {
            return 1.0D;
        }
        return 1.0D + Config.altarBlessingBoostPerAltar() * Math.min(count, cap);
    }

    /** Create 机械概率：3–8 本维度，9+ 源维度/数据文件目标维度；1–2 不启用。 */
    public static boolean mechanicalEnabled(ServerLevel level) {
        MinecraftServer server = level.getServer();
        if (server == null || !server.isSameThread()) {
            return false;
        }
        int own = AltarSavedData.get(level).activeSiteCount();
        if (own >= DIMENSION_WIDE_MIN) {
            return true;
        }
        return hasWorldSpreadSource(level, server);
    }

    /**
     * 祭坛维度丰收是否在指定作物位置生效：
     * <3 座只在该位置处于某个激活祭坛 32×25×32 范围内时生效；
     * 3–8 座本维度全范围；9+ 源/目标维度全范围。
     */
    public static boolean harvestEnabledAt(ServerLevel level, BlockPos pos) {
        MinecraftServer server = level.getServer();
        if (server == null || !server.isSameThread()) {
            return false;
        }
        int own = AltarSavedData.get(level).activeSiteCount();
        if (own >= DIMENSION_WIDE_MIN) {
            return true;
        }
        if (hasWorldSpreadSource(level, server)) {
            return true;
        }
        return own > 0 && AltarSavedData.get(level).findActiveSiteAt(pos) != null;
    }

    /** 9+ 源维度扩散：当前维度必须是数据文件目标维度，且其他维度存在 9+ 祭坛。 */
    private static boolean hasWorldSpreadSource(ServerLevel level, MinecraftServer server) {
        if (!AltarBoostData.isSpreadDimension(level.dimension().location())) {
            return false;
        }
        for (ServerLevel other : server.getAllLevels()) {
            if (AltarSavedData.get(other).activeSiteCount() >= WORLD_WIDE_MIN) {
                return true;
            }
        }
        return false;
    }

    private static int effectiveAltarCount(ServerPlayer player, ServerLevel level, MinecraftServer server) {
        int own = AltarSavedData.get(level).activeSiteCount();
        boolean target = AltarBoostData.isSpreadDimension(level.dimension().location());
        int sourceMax = 0;
        if (own >= WORLD_WIDE_MIN || target) {
            for (ServerLevel other : server.getAllLevels()) {
                int count = AltarSavedData.get(other).activeSiteCount();
                if (count >= WORLD_WIDE_MIN) {
                    sourceMax = Math.max(sourceMax, count);
                }
            }
            if (sourceMax >= WORLD_WIDE_MIN) {
                return sourceMax;
            }
        }
        if (own >= DIMENSION_WIDE_MIN) {
            return own;
        }
        if (own > 0 && AltarSavedData.get(level).findActiveSiteAt(player.blockPosition()) != null) {
            return own;
        }
        return 0;
    }
}
