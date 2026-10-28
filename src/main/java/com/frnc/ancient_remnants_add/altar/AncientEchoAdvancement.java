package com.frnc.ancient_remnants_add.altar;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import dev.obscuria.ancient_remnants.api.AncientRemnantsApi;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashSet;
import java.util.Set;

/**
 * 挑战进度「远古回响」。
 *
 * <p>检测玩家背包里是否有已解锁全部研究条目的巨石典籍；有则授予
 * {@code ancient_remnants_add:ancient_echo}。祭坛使用时也会调用一次 {@link #tryAward}，
 * 避免玩家刚合并完条目、还没等到下一次 tick 检查就被祭坛拒绝。
 */
public final class AncientEchoAdvancement {

    public static final ResourceLocation ADVANCEMENT_ID =
            AncientRemnantsAdd.id("ancient_echo");

    private static final ResourceLocation CODEX_ITEM_ID =
            new ResourceLocation(AncientRemnantsAdd.ANCIENT_REMNANTS_MOD_ID, "monolith_codex");

    /** 上游 ItemStackAdapter 使用的 NBT 键。 */
    private static final String STORED_RESEARCH_TAG = "ancient_remnants:stored_research";

    private static final String CRITERION = "unlocked_codex";

    private static Item codexItem;

    private AncientEchoAdvancement() {
    }

    /** 每 20 tick 检查一次，避免每 tick 扫描背包。 */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % 20 != 0) {
            return;
        }
        tryAward(player);
    }

    public static boolean tryAward(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }
        Advancement advancement = server.getAdvancements().getAdvancement(ADVANCEMENT_ID);
        if (advancement == null) {
            return false;
        }
        PlayerAdvancements progress = player.getAdvancements();
        if (progress.getOrStartProgress(advancement).isDone()) {
            return true;
        }
        if (!hasCompleteCodexInInventory(player)) {
            return false;
        }
        progress.award(advancement, CRITERION);
        return progress.getOrStartProgress(advancement).isDone();
    }

    /** 供祭坛右键检查：玩家手里这一本是否已解锁全部条目。 */
    public static boolean isCompleteCodex(ServerPlayer player, ItemStack stack) {
        Item item = codexItem();
        if (item == null || !stack.is(item)) {
            return false;
        }
        long total = researchCount(player);
        return total > 0L && isCompleteCodex(stack, total);
    }

    private static boolean hasCompleteCodexInInventory(ServerPlayer player) {
        Item item = codexItem();
        if (item == null) {
            return false;
        }
        long total = researchCount(player);
        if (total <= 0L) {
            return false;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item) && isCompleteCodex(stack, total)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (stack.is(item) && isCompleteCodex(stack, total)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCompleteCodex(ItemStack stack, long total) {
        CompoundTag tag = stack.getTag();
        if (tag == null) {
            return false;
        }
        ListTag entries = tag.getList(STORED_RESEARCH_TAG, Tag.TAG_STRING);
        if (entries.isEmpty()) {
            return false;
        }
        Set<String> unique = new HashSet<>();
        for (int i = 0; i < entries.size(); i++) {
            unique.add(entries.getString(i));
        }
        return unique.size() >= total;
    }

    private static long researchCount(ServerPlayer player) {
        try {
            return AncientRemnantsApi.INSTANCE
                    .listResearchEntries(player.level().registryAccess())
                    .count();
        } catch (RuntimeException exception) {
            return 0L;
        }
    }

    private static Item codexItem() {
        if (codexItem == null) {
            codexItem = ForgeRegistries.ITEMS.getValue(CODEX_ITEM_ID);
        }
        return codexItem;
    }
}
