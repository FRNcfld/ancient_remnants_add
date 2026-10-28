package com.frnc.ancient_remnants_add.mixin.create;

import com.frnc.ancient_remnants_add.create.CreateChanceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** 部署器/鼓风机等使用 RecipeApplier 的路径。 */
@Mixin(targets = "com.simibubi.create.foundation.recipe.RecipeApplier")
public abstract class MixinRecipeApplierChance {

    @Inject(
            method = "applyRecipeOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/Recipe;Z)Ljava/util/List;",
            at = @At("HEAD"), remap = false
    )
    private static void ancientRemnantsAdd$pushChanceContext(
            Level level, ItemStack stackIn, Recipe<?> recipe, boolean returnProcessingRemainder,
            CallbackInfoReturnable<List<ItemStack>> cir
    ) {
        CreateChanceContext.push(level);
    }

    @Inject(
            method = "applyRecipeOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/Recipe;Z)Ljava/util/List;",
            at = @At("RETURN"), remap = false
    )
    private static void ancientRemnantsAdd$popChanceContext(
            Level level, ItemStack stackIn, Recipe<?> recipe, boolean returnProcessingRemainder,
            CallbackInfoReturnable<List<ItemStack>> cir
    ) {
        CreateChanceContext.clear();
    }
}
