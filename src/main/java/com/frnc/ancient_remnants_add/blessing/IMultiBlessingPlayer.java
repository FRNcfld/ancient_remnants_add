package com.frnc.ancient_remnants_add.blessing;

import dev.obscuria.ancient_remnants.common.Blessing;
import net.minecraft.core.Holder;

import java.util.List;

/**
 * 由 {@code MixinPlayerBlessings} 注入到 {@link net.minecraft.world.entity.player.Player} 上的访问接口。
 *
 * <p>上游 1.5.2 的 {@code IPlayerAccess} 只能存一个祝福；这里在其旁边维护一个有序列表，
 * 列表本身由 Player 的 NBT 持久化。上游那个单祝福字段继续保留，并始终同步为「最近获得的祝福」，
 * 这样 Elderheart（方尖碑/纪念碑）原本的“已拥有就不再触发”判断仍然有效。
 */
public interface IMultiBlessingPlayer {

    /**
     * 返回玩家当前持有的祝福列表（按获得顺序，最旧在前）。
     * 返回的是可变列表，调用方可以直接增删；不要长期缓存。
     */
    List<Holder<Blessing>> ancientRemnantsAdd$getBlessings();

    /** 用给定列表整体替换当前祝福列表（用于死亡/重生时复制）。 */
    void ancientRemnantsAdd$setBlessings(List<Holder<Blessing>> blessings);
}
