package com.frnc.ancient_remnants_add.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 远古之心的 ROOT 方块实体。
 *
 * <p>数据库中的祭坛状态在 {@link AltarSavedData}；这里只作为客户端休眠核心渲染的锚点，
 * 不再保存典籍或注册活动祭坛。
 */
public class AncientHeartBlockEntity extends BlockEntity {

    public AncientHeartBlockEntity(BlockPos pos, BlockState state) {
        super(AltarRegistry.ANCIENT_HEART_BE.get(), pos, state);
    }
}
