package com.frnc.ancient_remnants_add.altar;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

/**
 * 祭坛相关进度：
 * <ul>
 *   <li>「过往的幻影」：探索进度，发现残缺祭坛（也可由 location 触发器自动获得）；</li>
 *   <li>「荣光再现」：挑战进度，激活祭坛（代码发放，父进度为前者）。</li>
 * </ul>
 */
public final class AltarAdvancements {

    public static final ResourceLocation RUIN_ID = AncientRemnantsAdd.id("ancient_heart_ruin");
    public static final String RUIN_CRITERION = "discover_altar";
    public static final ResourceLocation GLORY_ID = AncientRemnantsAdd.id("glory_reborn");
    public static final String GLORY_CRITERION = "awaken_altar";

    private AltarAdvancements() {
    }

    /** 激活祭坛时调用：先补发探索进度，保证挑战的父进度已满足。 */
    public static void awardGloryReborn(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        award(player, server, RUIN_ID, RUIN_CRITERION);
        award(player, server, GLORY_ID, GLORY_CRITERION);
    }

    private static boolean award(ServerPlayer player, MinecraftServer server,
                                 ResourceLocation id, String criterion) {
        Advancement advancement = server.getAdvancements().getAdvancement(id);
        if (advancement == null) {
            return false;
        }
        PlayerAdvancements progress = player.getAdvancements();
        if (progress.getOrStartProgress(advancement).isDone()) {
            return true;
        }
        progress.award(advancement, criterion);
        return progress.getOrStartProgress(advancement).isDone();
    }
}
