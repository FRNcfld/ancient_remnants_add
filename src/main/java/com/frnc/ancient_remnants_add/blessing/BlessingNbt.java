package com.frnc.ancient_remnants_add.blessing;

import com.mojang.serialization.Codec;
import dev.obscuria.ancient_remnants.common.Blessing;
import net.minecraft.core.Holder;

import java.util.List;

/** 多祝福列表的 NBT 键与编解码器。 */
public final class BlessingNbt {

    /** 与上游的 {@code AncientRemnantsBlessing}（单数）区分开。 */
    public static final String KEY = "AncientRemnantsAddBlessings";

    /**
     * 上游的 {@code Blessing.CODEC} 是按注册表名解析的 Holder codec，
     * 直接配合 {@code NbtOps} 使用即可，不需要 RegistryOps。
     */
    public static final Codec<List<Holder<Blessing>>> CODEC =
            Codec.list(Blessing.Companion.getCODEC());

    private BlessingNbt() {
    }
}
