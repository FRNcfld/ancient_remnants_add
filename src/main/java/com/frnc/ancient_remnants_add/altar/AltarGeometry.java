package com.frnc.ancient_remnants_add.altar;

import net.minecraft.core.BlockPos;

/**
 * 残缺祭坛模板的固定几何。
 *
 * <p>模板尺寸 32×25×32；2×2 远古之心的 ROOT 在模板局部 (15,3,15)，
 * 祭坛作用范围就是从模板 origin 到 origin + (31,24,31) 的整块结构。
 */
public final class AltarGeometry {

    public static final int HEART_X = 15;
    public static final int HEART_Y = 3;
    public static final int HEART_Z = 15;

    public static final int AREA_MIN_DX = -15;
    public static final int AREA_MIN_DY = -3;
    public static final int AREA_MIN_DZ = -15;
    public static final int AREA_MAX_DX = 16;
    public static final int AREA_MAX_DY = 21;
    public static final int AREA_MAX_DZ = 16;

    private AltarGeometry() {
    }

    /** 结构 origin（模板局部 0,0,0）→ 2×2 远古之心 ROOT。 */
    public static BlockPos siteRootFromOrigin(BlockPos origin) {
        return origin.offset(HEART_X, HEART_Y, HEART_Z);
    }

    /** 目标位置是否在祭坛作用范围内（含整块 32×25×32 结构）。 */
    public static boolean areaContains(BlockPos root, BlockPos pos) {
        return pos.getX() >= root.getX() + AREA_MIN_DX && pos.getX() <= root.getX() + AREA_MAX_DX
                && pos.getY() >= root.getY() + AREA_MIN_DY && pos.getY() <= root.getY() + AREA_MAX_DY
                && pos.getZ() >= root.getZ() + AREA_MIN_DZ && pos.getZ() <= root.getZ() + AREA_MAX_DZ;
    }
}
