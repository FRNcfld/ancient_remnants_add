package com.frnc.ancient_remnants_add.mixin.create;

import com.frnc.ancient_remnants_add.create.CreateChanceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 物品排放：覆盖可能带概率产出的排空配方。 */
@Mixin(targets = "com.simibubi.create.content.fluids.transfer.GenericItemEmptying")
public abstract class MixinGenericItemEmptyingChance {

    @Inject(method = "emptyItem", at = @At("HEAD"), remap = false)
    private static void ancientRemnantsAdd$pushChanceContext(
            Level world, ItemStack stack, boolean simulate, CallbackInfoReturnable<?> cir
    ) {
        CreateChanceContext.push(world);
    }

    @Inject(method = "emptyItem", at = @At("RETURN"), remap = false)
    private static void ancientRemnantsAdd$popChanceContext(
            Level world, ItemStack stack, boolean simulate, CallbackInfoReturnable<?> cir
    ) {
        CreateChanceContext.clear();
    }
}
