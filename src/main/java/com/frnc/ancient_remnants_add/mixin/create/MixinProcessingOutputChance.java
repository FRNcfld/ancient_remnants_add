package com.frnc.ancient_remnants_add.mixin.create;

import com.frnc.ancient_remnants_add.create.CreateChanceContext;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 在祭坛维度里，把所有 Create 概率产出的 chance 乘以 1.5（封顶 1.0）。 */
@Mixin(targets = "com.simibubi.create.content.processing.recipe.ProcessingOutput")
public abstract class MixinProcessingOutputChance {

    @Shadow(remap = false)
    @Final
    private float chance;

    @Redirect(
            method = "rollOutput",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/simibubi/create/content/processing/recipe/ProcessingOutput;chance:F",
                    remap = false
            ),
            remap = false
    )
    private float ancientRemnantsAdd$boostChance(ProcessingOutput output) {
        return CreateChanceContext.boost(this.chance);
    }
}
