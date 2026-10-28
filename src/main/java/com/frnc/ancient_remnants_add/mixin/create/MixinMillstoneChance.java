package com.frnc.ancient_remnants_add.mixin.create;

import com.frnc.ancient_remnants_add.create.CreateChanceContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 磨石：在概率产出掷骰期间设置祭坛维度上下文。 */
@Mixin(targets = "com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity")
public abstract class MixinMillstoneChance {

    @Inject(method = "process", at = @At("HEAD"), remap = false)
    private void ancientRemnantsAdd$pushChanceContext(CallbackInfo ci) {
        CreateChanceContext.push(((BlockEntity) (Object) this).getLevel());
    }

    @Inject(method = "process", at = @At("RETURN"), remap = false)
    private void ancientRemnantsAdd$popChanceContext(CallbackInfo ci) {
        CreateChanceContext.clear();
    }
}
