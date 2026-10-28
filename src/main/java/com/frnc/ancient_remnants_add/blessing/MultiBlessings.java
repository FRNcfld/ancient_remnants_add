package com.frnc.ancient_remnants_add.blessing;

import com.frnc.ancient_remnants_add.Config;
import dev.obscuria.ancient_remnants.access.IPlayerAccess;
import dev.obscuria.ancient_remnants.common.Blessing;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 多祝福列表的读写与效果同步。
 *
 * <p>上游的祝福效果是 MobEffect，每个祝福一个效果类型；同时持有多个祝福时，
 * 只要把每个祝福对应的效果都保持在玩家身上即可，不需要改动效果的实现。
 * 所有入口（Elderheart 授予、{@code /ancient_remnants blessing set/remove}）
 * 最终都会走 {@code AncientRemnantsApi.setBlessing}，由 mixin 转到这里。
 *
 * <p>这里是纯 Java 侧的工具类，所有对玩家注入接口的强转都先做 {@code instanceof} 判断：
 * mixin 万一没生效（被别的 mod 改了 mixin 配置、或 Injector 报错跳过），
 * 这些入口应该安静退出，而不是每 tick 抛 ClassCastException。
 */
public final class MultiBlessings {

    private MultiBlessings() {
    }

    /**
     * 获得一个祝福。重复获得时只刷新上游单祝福字段；达到上限时移除最早获得的那个，
     * 这也是本 mod 在满额时采用的策略。
     */
    public static void acquire(Player player, IMultiBlessingPlayer access, Holder<Blessing> blessing) {
        if (!(player instanceof IPlayerAccess upstream)) {
            return;
        }
        List<Holder<Blessing>> blessings = access.ancientRemnantsAdd$getBlessings();

        if (blessings.contains(blessing)) {
            upstream.ancientRemnants$setBlessing(blessing);
            refreshEffects(player, blessings);
            return;
        }

        int max = maxBlessings();
        while (blessings.size() >= max) {
            Holder<Blessing> oldest = blessings.remove(0);
            oldest.value().onRemoved(player);
        }

        blessings.add(blessing);
        // 同步上游的单祝福字段：Elderheart 靠它判断“这个祝福已经有了”。
        upstream.ancientRemnants$setBlessing(blessing);
        // setBlessing 会移除旧单祝福的效果，这里把列表里其余祝福的效果补回来。
        refreshEffects(player, blessings);
    }

    /** 清空全部祝福（对应上游的 {@code /ancient_remnants blessing remove}）。 */
    public static void clear(Player player, IMultiBlessingPlayer access) {
        if (!(player instanceof IPlayerAccess upstream)) {
            return;
        }
        List<Holder<Blessing>> blessings = access.ancientRemnantsAdd$getBlessings();
        for (Holder<Blessing> blessing : blessings) {
            blessing.value().onRemoved(player);
        }
        blessings.clear();
        upstream.ancientRemnants$setBlessing(null);
    }

    /**
     * 每个服务端玩家 tick 调用：整理列表、与上游单祝福字段保持一致、补上缺失的效果。
     * 也负责把旧存档里上游已存的单个祝福收编进列表。
     */
    public static void sync(ServerPlayer player) {
        if (!(player instanceof IMultiBlessingPlayer access) || !(player instanceof IPlayerAccess upstream)) {
            return;
        }
        List<Holder<Blessing>> blessings = access.ancientRemnantsAdd$getBlessings();
        int max = maxBlessings();

        // 去掉可能的重复项（例如旧 NBT 被手工改过）。单次 HashSet 扫描，避免每 tick 的 O(n²)。
        if (blessings.size() > 1) {
            Set<Holder<Blessing>> unique = new LinkedHashSet<>(blessings);
            if (unique.size() != blessings.size()) {
                blessings.clear();
                blessings.addAll(unique);
            }
        }

        // 配置调小后，多出来的部分按“最旧”顺序移除。
        while (blessings.size() > max) {
            blessings.remove(0).value().onRemoved(player);
        }

        Holder<Blessing> active = upstream.ancientRemnants$getBlessing();

        if (active == null) {
            if (!blessings.isEmpty()) {
                upstream.ancientRemnants$setBlessing(blessings.get(blessings.size() - 1));
            }
        } else if (!blessings.contains(active)) {
            if (blessings.size() < max) {
                // 旧存档：上游只存了一个祝福，把它收编进列表。
                blessings.add(active);
            } else {
                // 列表已满却还有列表外的单祝福，把上游字段换回列表中的一项。
                upstream.ancientRemnants$setBlessing(blessings.get(blessings.size() - 1));
            }
        }

        refreshEffects(player, blessings);
    }

    /** 玩家是否持有指定 id 的祝福（按注册名比较，避免依赖具体的 Holder 实例）。 */
    public static boolean has(Player player, ResourceLocation blessingId) {
        if (!(player instanceof IMultiBlessingPlayer access)) {
            return false;
        }
        for (Holder<Blessing> blessing : access.ancientRemnantsAdd$getBlessings()) {
            if (blessing.unwrapKey()
                    .map(key -> key.location())
                    .filter(blessingId::equals)
                    .isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static void refreshEffects(Player player, List<Holder<Blessing>> blessings) {
        for (Holder<Blessing> blessing : blessings) {
            blessing.value().onTick(player);
        }
    }

    private static int maxBlessings() {
        return Math.max(1, Config.maxBlessings());
    }
}
