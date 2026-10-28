package com.frnc.ancient_remnants_add.mixin;

import com.frnc.ancient_remnants_add.blessing.IMultiBlessingPlayer;
import com.frnc.ancient_remnants_add.blessing.MultiBlessings;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.obscuria.ancient_remnants.server.BlessingCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让上游的 {@code /ancient_remnants blessing remove} 清空多祝福列表。
 *
 * <p>不能简单在 {@code AncientRemnantsApi.setBlessing(null)} 里清空：上游的
 * {@code Blessing.onRestore} 也会传 null（旧玩家没有单祝福时），那样会把重生时
 * 刚复制的列表一起抹掉。所以只在命令这一层拦截。
 */
@Mixin(BlessingCommands.class)
public abstract class MixinBlessingCommands {

    @Inject(method = "removeBlessing", at = @At("HEAD"), cancellable = true, remap = false)
    private void ancientRemnantsAdd$removeBlessing(CommandContext<CommandSourceStack> command,
                                                   CallbackInfoReturnable<Integer> cir) {
        final ServerPlayer player;
        try {
            player = EntityArgument.getPlayer(command, "player");
        } catch (CommandSyntaxException exception) {
            cir.setReturnValue(0);
            return;
        }
        if (!(player instanceof IMultiBlessingPlayer access)) {
            return;
        }
        MultiBlessings.clear(player, access);
        cir.setReturnValue(1);
    }
}
