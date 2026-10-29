package com.frnc.ancient_remnants_add.altar;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.frnc.ancient_remnants_add.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 远古之心生效期间，判断哪些方块算「作物」，以及是否要额外掉一份。
 *
 * <p>覆盖范围由数据包标签 {@code ancient_remnants_add:altar_harvestable} 决定：
 * 默认包含原版所有农作物的成熟方块、下界疣、甘蔗、甜浆果、可可、竹子、仙人掌、
 * 海带、西瓜和南瓜；其他 mod 的作物可以往这个标签里加。
 */
public final class AltarHarvestHelper {

    public static final TagKey<Block> ALTAR_HARVESTABLE =
            BlockTags.create(new ResourceLocation(AncientRemnantsAdd.MOD_ID, "altar_harvestable"));

    private static final ResourceLocation BOUNTIFUL_HARVEST =
            new ResourceLocation(AncientRemnantsAdd.ANCIENT_REMNANTS_MOD_ID, "bountiful_harvest");

    private static MobEffect bountifulHarvestEffect;

    private AltarHarvestHelper() {
    }

    public static boolean isHarvestable(BlockState state) {
        if (!state.is(ALTAR_HARVESTABLE)) {
            return false;
        }
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        if (block instanceof SweetBerryBushBlock) {
            return state.getValue(SweetBerryBushBlock.AGE) >= 2;
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= 2;
        }
        // 甘蔗、竹子、仙人掌、海带、西瓜、南瓜以及后续加入标签的作物：不额外做成熟度判断。
        return true;
    }

    /**
     * 玩家自己带丰收祝福时，原版的上游 hook 已经会对成熟的 CropBlock / NetherWart 再掉一份；
     * 这种情况下跳过祭坛的额外掉落，避免同一次破坏触发两次。
     */
    public static boolean shouldSkipForPersonalBlessing(Entity entity, BlockState state) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        MobEffect effect = bountifulHarvestEffect();
        if (effect == null || !player.hasEffect(effect)) {
            return false;
        }
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= 3;
        }
        return false;
    }

    public static boolean rollChance(ServerLevel level) {
        return level.random.nextFloat() < (float) Config.bountifulHarvestChance();
    }

    public static List<ItemStack> duplicateDrops(List<ItemStack> drops) {
        List<ItemStack> result = new ArrayList<>(drops.size() * 2);
        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                result.add(stack);
            }
        }
        for (ItemStack stack : drops) {
            if (!stack.isEmpty()) {
                result.add(stack.copy());
            }
        }
        return result;
    }

    /** 供祭坛/丰收 mixin 复用；注册表未就绪时返回 null。 */
    public static MobEffect bountifulHarvestEffect() {
        if (bountifulHarvestEffect == null) {
            bountifulHarvestEffect = ForgeRegistries.MOB_EFFECTS.getValue(BOUNTIFUL_HARVEST);
        }
        return bountifulHarvestEffect;
    }
}
