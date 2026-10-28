package com.frnc.ancient_remnants_add.mixin.create;

import com.frnc.ancient_remnants_add.create.CreateChanceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 注液器：覆盖可能带概率产出的灌装配方。 */
@Mixin(targets = "com.simibubi.create.content.fluids.spout.FillingBySpout")
public abstract class MixinFillingBySpoutChance {

    @Inject(method = "fillItem", at = @At("HEAD"), remap = false)
    private static void ancientRemnantsAdd$pushChanceContext(
            Level world, int requiredAmount, ItemStack stack, FluidStack availableFluid,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        CreateChanceContext.push(world);
    }

    @Inject(method = "fillItem", at = @At("RETURN"), remap = false)
    private static void ancientRemnantsAdd$popChanceContext(
            Level world, int requiredAmount, ItemStack stack, FluidStack availableFluid,
            CallbackInfoReturnable<ItemStack> cir
    ) {
        CreateChanceContext.clear();
    }
}
