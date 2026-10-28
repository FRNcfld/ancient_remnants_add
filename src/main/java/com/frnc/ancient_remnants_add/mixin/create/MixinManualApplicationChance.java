package com.frnc.ancient_remnants_add.mixin.create;

import com.frnc.ancient_remnants_add.create.CreateChanceContext;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 手动应用配方（右键方块）：在概率产出掷骰期间设置祭坛维度上下文。 */
@Mixin(targets = "com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe")
public abstract class MixinManualApplicationChance {

    @Inject(method = "manualApplicationRecipesApplyInWorld", at = @At("HEAD"), remap = false)
    private static void ancientRemnantsAdd$pushChanceContext(
            PlayerInteractEvent.RightClickBlock event, CallbackInfo ci
    ) {
        CreateChanceContext.push(event.getLevel());
    }

    @Inject(method = "manualApplicationRecipesApplyInWorld", at = @At("RETURN"), remap = false)
    private static void ancientRemnantsAdd$popChanceContext(
            PlayerInteractEvent.RightClickBlock event, CallbackInfo ci
    ) {
        CreateChanceContext.clear();
    }
}
