package com.frnc.ancient_remnants_add.mixin.create;

import com.frnc.ancient_remnants_add.create.CreateChanceContext;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 工作盆混合：在概率产出掷骰期间设置祭坛维度上下文。 */
@Mixin(targets = "com.simibubi.create.content.processing.basin.BasinRecipe")
public abstract class MixinBasinRecipeChance {

    @Inject(
            method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
            at = @At("HEAD"), remap = false
    )
    private static void ancientRemnantsAdd$pushChanceContext(
            BasinBlockEntity basin, Recipe<?> recipe, boolean test, CallbackInfoReturnable<Boolean> cir
    ) {
        CreateChanceContext.push(basin.getLevel());
    }

    @Inject(
            method = "apply(Lcom/simibubi/create/content/processing/basin/BasinBlockEntity;Lnet/minecraft/world/item/crafting/Recipe;Z)Z",
            at = @At("RETURN"), remap = false
    )
    private static void ancientRemnantsAdd$popChanceContext(
            BasinBlockEntity basin, Recipe<?> recipe, boolean test, CallbackInfoReturnable<Boolean> cir
    ) {
        CreateChanceContext.clear();
    }
}
