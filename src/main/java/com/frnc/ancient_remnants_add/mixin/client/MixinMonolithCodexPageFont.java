package com.frnc.ancient_remnants_add.mixin.client;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.UnaryOperator;

/**
 * 修复远古遗迹典籍界面在中文（以及其他非拉丁语言）下显示方框的问题。
 *
 * <p>上游 {@code MonolithCodexScreen.Page.applyCodexStyle} 会把研究标题/正文的 Style
 * 强制换成 {@code ancient_remnants:header} 的 TTF 字体；这个字体没有中文字形，
 * 所以中文全是 missing glyph 方框。这里把字体换成 {@code ancient_remnants_add:codex}：
 * 拉丁字符继续用上游 header.ttf，缺字时回退到 {@code minecraft:default}。
 *
 * <p>注入点选 {@code applyCodexStyle} 这个**命名方法**，而不是它编译出来的
 * {@code applyCodexStyle$lambda$0}：后者是 Kotlin 编译器生成的合成方法名，
 * 上游改一行代码就可能变，而命名方法稳定得多。
 * 具体拦的是 {@code MutableComponent#withStyle(UnaryOperator)}：先执行上游自己的变换，
 * 再把字体覆盖成我们的——这样上游将来往 lambda 里加别的样式也不会被吞掉。
 *
 * <p>注入用 {@code require = 0}：这只是显示层修饰，上游改结构时宁可在日志里留一条 warn
 * 并静默失效，也不要让客户端因为找不到注入点直接崩溃。
 *
 * <p>只影响客户端界面；服务端不会加载该 mixin。
 */
@Mixin(targets = "dev.obscuria.ancient_remnants.client.MonolithCodexScreen$Page")
public abstract class MixinMonolithCodexPageFont {

    @Redirect(
            method = "applyCodexStyle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/chat/MutableComponent;withStyle(Ljava/util/function/UnaryOperator;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            require = 0
    )
    private MutableComponent ancientRemnantsAdd$useCodexFont(
            MutableComponent component, UnaryOperator<Style> upstreamStyle
    ) {
        return component.withStyle(style -> upstreamStyle.apply(style).withFont(AncientRemnantsAdd.id("codex")));
    }
}
