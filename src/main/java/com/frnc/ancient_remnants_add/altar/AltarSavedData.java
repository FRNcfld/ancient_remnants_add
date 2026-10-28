package com.frnc.ancient_remnants_add.altar;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 每个维度一份的活动祭坛位置表。
 *
 * <p>祭坛效果是维度级的，不能只依赖区块加载：区块卸载后祭坛仍然存在。这里把激活的
 * 祭坛位置写入世界存档，作物掉落检查只关心该维度是否有任意活动祭坛。
 */
public final class AltarSavedData extends SavedData {

    private static final String DATA_NAME = "ancient_remnants_add_altars";
    private static final String ALTARS_TAG = "Altars";

    private final Set<BlockPos> activeAltars = new HashSet<>();

    private AltarSavedData() {
    }

    public static AltarSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(AltarSavedData::load, AltarSavedData::new, DATA_NAME);
    }

    public static AltarSavedData load(CompoundTag tag) {
        AltarSavedData data = new AltarSavedData();
        ListTag list = tag.getList(ALTARS_TAG, Tag.TAG_LONG);
        for (int i = 0; i < list.size(); i++) {
            Tag element = list.get(i);
            if (element instanceof LongTag longTag) {
                data.activeAltars.add(BlockPos.of(longTag.getAsLong()));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (BlockPos pos : activeAltars) {
            list.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put(ALTARS_TAG, list);
        return tag;
    }

    /**
     * 该维度里是否还有仍然有效的激活祭坛。
     *
     * <p>不能只判断集合非空：存档里可能残留已经不存在的祭坛位置（崩溃、别的模组直接改方块、
     * 存档回滚），那样会让整个维度永久获得作物双掉落与 Create 概率加成，而清理入口只有
     * 右键取回和方块被移除两条。所以这里逐条校验：区块已加载时要求该位置仍然是激活的
     * 巨石祭坛，否则删掉这条记录；区块未加载时无法判断，按仍然有效处理
     * （方块实体重新加载时会由 {@code setLevel} 再登记一次）。
     */
    public boolean isActive(ServerLevel level) {
        if (activeAltars.isEmpty()) {
            return false;
        }
        boolean active = false;
        boolean changed = false;
        Iterator<BlockPos> iterator = activeAltars.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (!level.isLoaded(pos)) {
                active = true;
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() == AltarRegistry.MONOLITH_ALTAR.get()
                    && state.getValue(MonolithAltarBlock.ACTIVE)) {
                active = true;
            } else {
                iterator.remove();
                changed = true;
                AncientRemnantsAdd.debug("丢弃失效的祭坛记录: {}", pos.toShortString());
            }
        }
        if (changed) {
            setDirty();
        }
        return active;
    }

    public void add(BlockPos pos) {
        if (activeAltars.add(pos)) {
            setDirty();
        }
    }

    public void remove(BlockPos pos) {
        if (activeAltars.remove(pos)) {
            setDirty();
        }
    }

    public boolean contains(BlockPos pos) {
        return activeAltars.contains(pos);
    }
}
