package com.frnc.ancient_remnants_add.blessing;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/** 上游六种祝福的注册 ID；祭坛加成数据文件也用这些 ID。 */
public final class BlessingIds {

    public static final ResourceLocation BOUNTIFUL_HARVEST = id("bountiful_harvest");
    public static final ResourceLocation HUNTER_INSTINCTS = id("hunter_instincts");
    public static final ResourceLocation ARCHITECT_REACH = id("architect_reach");
    public static final ResourceLocation DESPERATE_VITALITY = id("desperate_vitality");
    public static final ResourceLocation WARRIOR_WRATH = id("warrior_wrath");
    public static final ResourceLocation SENTINEL_AEGIS = id("sentinel_aegis");

    public static final Set<ResourceLocation> ALL = Set.of(
            BOUNTIFUL_HARVEST,
            HUNTER_INSTINCTS,
            ARCHITECT_REACH,
            DESPERATE_VITALITY,
            WARRIOR_WRATH,
            SENTINEL_AEGIS
    );

    private BlessingIds() {
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(AncientRemnantsAdd.ANCIENT_REMNANTS_MOD_ID, path);
    }
}
