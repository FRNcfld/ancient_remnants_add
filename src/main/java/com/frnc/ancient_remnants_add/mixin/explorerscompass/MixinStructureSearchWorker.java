package com.frnc.ancient_remnants_add.mixin.explorerscompass;

import com.frnc.ancient_remnants_add.altar.AltarSavedData;
import com.frnc.ancient_remnants_add.worldgen.AncientHeartRuinStructure;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 结构指南针（Explorer's Compass）联动：搜索时跳过已经激活的残缺祭坛。
 *
 * <p>已激活的祭坛属于玩家的据点，不应该被结构指南针当作探索目标指路。搜索 worker 在
 * 服务端主线程（Forge {@code WorldWorkerManager}）逐个候选区块调用
 * {@code getStructureGeneratingAt}；这里在返回处把「属于本模组结构、且该区块已有激活站点」
 * 的结果改成 {@code null}，worker 会自然继续向外找未激活的遗迹。
 *
 * <p>注入点选 {@code getStructureGeneratingAt} 而不是 {@code shouldIgnore}：
 * 前者能拿到具体的 {@link Structure}，过滤只针对本模组的 {@code AncientHeartRuinStructure}，
 * 不会按坐标一刀切、误伤同区块里其他结构类型的搜索结果。三个搜索 worker
 * （RandomSpread / ConcentricRings / Generic）都经过这个方法。
 *
 * <p>Explorer's Compass 是软依赖（见 {@code mods.toml}），该 mixin 由
 * {@code AncientRemnantsAddMixinPlugin} 在它不存在时跳过；类本身不直接引用上游类型
 * （只把目标类名写在 {@code @Mixin(targets = ...)} 里），所以没装它也能正常加载本类。
 *
 * <p>{@code require = 0}：这是可选模组的内部方法，上游改版导致注入点消失时宁可让指南针
 * 退回原版行为，也不要让游戏启动崩溃。版本以 {@code gradle.properties} 的
 * {@code explorers_compass_version}（1.20.1-1.4.0）为准。
 */
@Mixin(targets = "com.chaosthedude.explorerscompass.worker.StructureSearchWorker")
public abstract class MixinStructureSearchWorker {

    @Shadow(remap = false)
    protected ServerLevel level;

    @Inject(method = "getStructureGeneratingAt", at = @At("RETURN"), remap = false, require = 0, cancellable = true)
    private void ancientRemnantsAdd$skipActivatedAltar(
            ChunkPos chunkPos, CallbackInfoReturnable<Pair<BlockPos, Structure>> cir
    ) {
        Pair<BlockPos, Structure> found = cir.getReturnValue();
        if (found == null || !(found.getSecond() instanceof AncientHeartRuinStructure)) {
            return;
        }
        if (AltarSavedData.get(level).hasActiveSiteInChunk(chunkPos.x, chunkPos.z)) {
            cir.setReturnValue(null);
        }
    }
}
