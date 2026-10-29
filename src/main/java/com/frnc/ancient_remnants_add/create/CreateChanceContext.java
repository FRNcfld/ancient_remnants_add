package com.frnc.ancient_remnants_add.create;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.frnc.ancient_remnants_add.altar.AltarBoostScope;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Create 概率产出 +50% 的上下文。
 *
 * <p>Create 的加工机器没有玩家上下文，所以这里以「该维度的祭坛层级」作为触发条件：
 * <ul>
 *   <li>1–2 座激活祭坛：不启用；</li>
 *   <li>3–8 座：该维度内所有概率产出 ×1.5（封顶 100%）；</li>
 *   <li>9+ 座：源维度 + 数据文件列出的扩散维度都启用。</li>
 * </ul>
 * 具体范围判断见 {@link AltarBoostScope#mechanicalEnabled(ServerLevel)}。
 *
 * <p>各个 Create 调用点在 {@code ProcessingRecipe.rollResults()} 前后通过
 * {@link #push(Level)} / {@link #clear()} 设置线程上下文；没有上下文的概率掷骰
 * （例如 JEI 预览）不会被增强。
 *
 * <p>上下文按层计数：嵌套调用时内层 {@link #clear()} 只退一层，不会把外层的开关抹掉。
 * 由于 mixin 只能在目标方法的 HEAD / RETURN 上成对注入，目标方法抛异常时 RETURN 不会执行，
 * 所以额外补一道安全网——每个服务端 tick 结束时统一清空（{@link #onServerTick}）。
 * 漏出的上下文最多持续到本次 tick 结束，不会把之后所有概率掷骰都加成。
 */
@Mod.EventBusSubscriber(modid = AncientRemnantsAdd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CreateChanceContext {

    private static final ThreadLocal<Boolean> BOOST = new ThreadLocal<>();
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private static final float MULTIPLIER = 1.5F;

    private CreateChanceContext() {
    }

    /** 进入一次可能掷概率的 Create 调用；只记录最外层（深度 0）的维度判断结果。 */
    public static void push(Level level) {
        int depth = DEPTH.get();
        if (depth == 0) {
            BOOST.set(level instanceof ServerLevel serverLevel
                    && AltarBoostScope.mechanicalEnabled(serverLevel));
        }
        DEPTH.set(depth + 1);
    }

    /** 退出一层；深度归零时清掉本线程的开关。 */
    public static void clear() {
        int depth = DEPTH.get() - 1;
        if (depth <= 0) {
            DEPTH.remove();
            BOOST.remove();
        } else {
            DEPTH.set(depth);
        }
    }

    public static float boost(float chance) {
        if (chance >= 1.0F || !Boolean.TRUE.equals(BOOST.get())) {
            return chance;
        }
        return Math.min(1.0F, chance * MULTIPLIER);
    }

    /**
     * 安全网：服务端 tick 结束时，线程栈上不可能还有正在掷骰的 Create 机器，
     * 残留的上下文一律丢弃（异常路径、mixin 注入被跳过等情况都在这里兜住）。
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        DEPTH.remove();
        BOOST.remove();
    }
}
