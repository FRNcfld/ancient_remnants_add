package com.frnc.ancient_remnants_add.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 每个维度一份的祭坛站点表。
 *
 * <p>站点以远古之心 ROOT 为主键；记录「是否修复、是否激活、激活时消耗的典籍」。
 * 站点由结构生成时的 {@code afterPlace} 登记（服务端主线程），或由玩家放置远古之心时的
 * fallback 补登记；即使远古之心被破坏/献祭销毁，站点记录也保留，允许重新修复和激活。
 *
 * <p>所有变更都必须在服务端主线程执行；区块生成线程（{@code Structure.afterPlace}）需要先
 * {@code server.execute(...)} 再调用这里。
 */
public final class AltarSavedData extends SavedData {

    private static final String DATA_NAME = "ancient_remnants_add_altar_sites";
    private static final String SITES_TAG = "Sites";

    private final Map<BlockPos, SiteRecord> sites = new HashMap<>();
    /** 热路径索引：只遍历激活站点；与 sites 中的 active 同步维护。 */
    private final Set<BlockPos> activeRoots = new HashSet<>();

    private AltarSavedData() {
    }

    public static AltarSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(AltarSavedData::load, AltarSavedData::new, DATA_NAME);
    }

    public static AltarSavedData load(CompoundTag tag) {
        AltarSavedData data = new AltarSavedData();
        ListTag list = tag.getList(SITES_TAG, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            SiteRecord site = SiteRecord.load(list.getCompound(i));
            if (site != null) {
                data.sites.put(site.root(), site);
                if (site.active()) {
                    data.activeRoots.add(site.root());
                }
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (SiteRecord site : sites.values()) {
            list.add(site.save());
        }
        tag.put(SITES_TAG, list);
        return tag;
    }

    /** 结构生成时登记一个站点；幂等。 */
    public void registerSite(BlockPos root) {
        if (sites.containsKey(root)) {
            return;
        }
        sites.put(root.immutable(), new SiteRecord(root.immutable()));
        setDirty();
    }

    /** 玩家把 2×2 远古之心放进站点后标记为已修复。 */
    public boolean markRepaired(BlockPos root) {
        SiteRecord site = sites.get(root);
        if (site == null) {
            return false;
        }
        if (!site.repaired) {
            site.repaired = true;
            setDirty();
        }
        return true;
    }

    /** 激活站点并保存消耗的典籍；要求已修复且未激活。 */
    public boolean activate(BlockPos root, ItemStack codex) {
        SiteRecord site = sites.get(root);
        if (site == null || !site.repaired || site.active) {
            return false;
        }
        site.active = true;
        site.codex = codex.copyWithCount(1);
        activeRoots.add(root);
        setDirty();
        return true;
    }

    /**
     * 熄灭站点：清除激活/修复状态并返回保存的典籍（调用方负责掉落）。
     * 站点记录本身保留，玩家可以重新放远古之心再次激活。
     */
    public ItemStack extinguish(BlockPos root) {
        SiteRecord site = sites.get(root);
        if (site == null) {
            return ItemStack.EMPTY;
        }
        ItemStack codex = site.codex.copy();
        boolean changed = false;
        if (site.active) {
            site.active = false;
            activeRoots.remove(root);
            changed = true;
        }
        if (site.repaired) {
            site.repaired = false;
            changed = true;
        }
        if (!site.codex.isEmpty()) {
            site.codex = ItemStack.EMPTY;
            changed = true;
        }
        if (changed) {
            setDirty();
        }
        return codex;
    }

    public SiteRecord findSiteAt(BlockPos pos) {
        for (SiteRecord site : sites.values()) {
            if (site.contains(pos)) {
                return site;
            }
        }
        return null;
    }

    public SiteRecord findRepairableSiteAt(BlockPos pos) {
        SiteRecord site = findSiteAt(pos);
        return site != null && site.repaired && !site.active ? site : null;
    }

    public SiteRecord findActiveSiteAt(BlockPos pos) {
        for (BlockPos root : activeRoots) {
            if (AltarGeometry.areaContains(root, pos)) {
                return sites.get(root);
            }
        }
        return null;
    }

    /**
     * 该区块内是否有已激活的祭坛站点。
     *
     * <p>残缺祭坛恰好占一个区块（结构 origin 对齐区块最小角，ROOT 在 origin + (15,3,15)），
     * 所以用 ROOT 所在区块即可判断整座祭坛；供结构指南针跳过已激活的祭坛。
     */
    public boolean hasActiveSiteInChunk(int chunkX, int chunkZ) {
        for (BlockPos root : activeRoots) {
            if ((root.getX() >> 4) == chunkX && (root.getZ() >> 4) == chunkZ) {
                return true;
            }
        }
        return false;
    }

    /** 激活祭坛覆盖的 32×25×32 范围不可破坏。 */
    public boolean isProtected(BlockPos pos) {
        for (BlockPos root : activeRoots) {
            if (AltarGeometry.areaContains(root, pos)) {
                return true;
            }
        }
        return false;
    }

    /** 该维度当前激活祭坛数；只允许在服务端主线程读取。 */
    public int activeSiteCount() {
        return activeRoots.size();
    }

    public boolean hasActiveSite() {
        return !activeRoots.isEmpty();
    }

    /** 维度里是否有任意激活祭坛；供作物双掉与 Create 概率加成复用。 */
    public boolean isActive(ServerLevel level) {
        return hasActiveSite();
    }

    /** 单个站点的持久化状态。 */
    public static final class SiteRecord {

        private static final String ROOT_TAG = "Root";
        private static final String REPAIRED_TAG = "Repaired";
        private static final String ACTIVE_TAG = "Active";
        private static final String CODEX_TAG = "Codex";

        private final BlockPos root;
        private boolean repaired;
        private boolean active;
        private ItemStack codex = ItemStack.EMPTY;

        private SiteRecord(BlockPos root) {
            this.root = root;
        }

        public BlockPos root() {
            return root;
        }

        public boolean repaired() {
            return repaired;
        }

        public boolean active() {
            return active;
        }

        public ItemStack codex() {
            return codex.copy();
        }

        public boolean contains(BlockPos pos) {
            return AltarGeometry.areaContains(root, pos);
        }

        private CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putLong(ROOT_TAG, root.asLong());
            tag.putBoolean(REPAIRED_TAG, repaired);
            tag.putBoolean(ACTIVE_TAG, active);
            if (!codex.isEmpty()) {
                tag.put(CODEX_TAG, codex.save(new CompoundTag()));
            }
            return tag;
        }

        private static SiteRecord load(CompoundTag tag) {
            if (!tag.contains(ROOT_TAG, Tag.TAG_LONG)) {
                return null;
            }
            SiteRecord site = new SiteRecord(BlockPos.of(tag.getLong(ROOT_TAG)));
            site.repaired = tag.getBoolean(REPAIRED_TAG);
            site.active = tag.getBoolean(ACTIVE_TAG);
            if (tag.contains(CODEX_TAG, Tag.TAG_COMPOUND)) {
                site.codex = ItemStack.of(tag.getCompound(CODEX_TAG));
            }
            return site;
        }
    }
}
