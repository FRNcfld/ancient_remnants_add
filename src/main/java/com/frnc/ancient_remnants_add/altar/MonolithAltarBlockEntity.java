package com.frnc.ancient_remnants_add.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 保存祭坛上放置的巨石典籍，激活期间用于空手右键取回；客户端不再渲染典籍。 */
public class MonolithAltarBlockEntity extends BlockEntity {

    private static final String CODEX_TAG = "Codex";

    private ItemStack codex = ItemStack.EMPTY;

    public MonolithAltarBlockEntity(BlockPos pos, BlockState state) {
        super(AltarRegistry.MONOLITH_ALTAR_BE.get(), pos, state);
    }

    /** 返回副本：调用方（取回典籍、破坏掉落）改不到方块实体持有的那一份。 */
    public ItemStack getCodex() {
        return codex.copy();
    }

    public void setCodex(ItemStack stack) {
        this.codex = stack.copyWithCount(1);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!codex.isEmpty()) {
            tag.put(CODEX_TAG, codex.save(new CompoundTag()));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        // 走 setCodex 而不是直接赋值：/data merge 或旧存档可能写入 Count>1，
        // 只有 setCodex 会把数量钳到 1，否则空手取回能整组拿出典籍。
        setCodex(tag.contains(CODEX_TAG) ? ItemStack.of(tag.getCompound(CODEX_TAG)) : ItemStack.EMPTY);
    }

    // 刻意不覆写 getUpdateTag / getUpdatePacket：客户端不渲染典籍，也不需要这份物品 NBT。
    // 激活状态由方块状态同步（MonolithAltarBlock.use 里的 sendBlockUpdated）就够了，
    // 否则每次方块更新 / 区块同步都要白传一份物品数据。

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        // 区块重新加载时，如果祭坛仍是激活状态，把它重新登记到该维度的活动祭坛集合。
        if (level instanceof ServerLevel serverLevel
                && getBlockState().getValue(MonolithAltarBlock.ACTIVE)) {
            AltarSavedData.get(serverLevel).add(worldPosition);
        }
    }
}
