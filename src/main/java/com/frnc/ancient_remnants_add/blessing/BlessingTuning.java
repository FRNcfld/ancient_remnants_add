package com.frnc.ancient_remnants_add.blessing;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.frnc.ancient_remnants_add.Config;
import dev.obscuria.ancient_remnants.access.IPlayerAccess;
import dev.obscuria.ancient_remnants.common.Blessing;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.List;
import java.util.UUID;

/**
 * 需要「后处理」的祝福数值。
 *
 * <p>五个效果类的数值都在触发时通过 mixin 读取 {@link Config}，改配置即时生效；
 * 只有建筑师之触是在效果对象构造时把 {@code +3} 写进 MobEffect 的属性修正器表。
 * 效果构造早于配置加载，所以这里在配置加载/注册完成后把表项替换成配置值。
 * 服务器已启动时，还会清掉在线玩家身上的旧修正器并临时移除该效果，让下一 tick 的
 * 多祝福同步用新数值重新挂上（否则 AttributeMap 里保留的是旧修正器，越重载越高）。
 */
public final class BlessingTuning {

    private static final ResourceLocation ARCHITECT_REACH =
            new ResourceLocation(AncientRemnantsAdd.ANCIENT_REMNANTS_MOD_ID, "architect_reach");

    /** 与上游 ArchitectReachEffect 构造函数里使用的 UUID 保持一致（上游改用 addAttributeModifier(..., "839056a5-...")）。 */
    private static final UUID ARCHITECT_REACH_MODIFIER_ID =
            UUID.fromString("839056a5-f3ae-4e48-ae6c-96d38db30a38");

    private BlessingTuning() {
    }

    public static void applyArchitectReach() {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(ARCHITECT_REACH);
        if (effect == null) {
            // 注册表尚未就绪；commonSetup 会再调用一次。
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            // 启动阶段（commonSetup）：没有在线玩家，直接改效果表即可。
            apply(effect, null);
            return;
        }
        // 配置重载阶段：把修改和在线玩家刷新都放到服务端线程。
        server.execute(() -> apply(effect, server));
    }

    private static void apply(MobEffect effect, MinecraftServer server) {
        if (!ForgeMod.BLOCK_REACH.isPresent()) {
            return;
        }
        Attribute attribute = ForgeMod.BLOCK_REACH.get();
        effect.getAttributeModifiers().put(attribute, new AttributeModifier(
                ARCHITECT_REACH_MODIFIER_ID,
                effect::getDescriptionId,
                Config.architectReachBonus(),
                AttributeModifier.Operation.ADDITION
        ));

        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // 先清旧修正器，再决定要不要重挂：这一步与玩家当前是否持有祝福无关，
            // 因为旧 UUID 的修正器可能已经留在 AttributeMap 里了。
            removeStaleReachModifiers(player, attribute, effect);

            if (!player.hasEffect(effect)) {
                continue;
            }
            boolean holdsBlessing = MultiBlessings.has(player, ARCHITECT_REACH)
                    || (player instanceof IPlayerAccess access && isArchitectReach(access.ancientRemnants$getBlessing()));
            if (holdsBlessing) {
                // 下一 tick 的 BlessingEvents 会用新的属性修正器重新挂上。
                player.removeEffect(effect);
            }
        }
    }

    /**
     * 删掉玩家身上「名字属于本效果」的触及距离修正器。
     *
     * <p>不能按 UUID 删：上游一旦换了 UUID，旧修正器就留在 AttributeMap 里，光靠
     * {@code removeEffect} 移不掉，于是每次配置重载都会再叠一层加成。
     * 按名字匹配可以把旧 UUID 的那份一起清掉，同时不碰其它模组来源的修正器
     * （那些名字不同）。
     */
    private static void removeStaleReachModifiers(ServerPlayer player, Attribute attribute, MobEffect effect) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        String modifierName = effect.getDescriptionId();
        for (AttributeModifier modifier : List.copyOf(instance.getModifiers())) {
            if (modifierName.equals(modifier.getName())) {
                instance.removeModifier(modifier);
            }
        }
    }

    private static boolean isArchitectReach(Holder<Blessing> blessing) {
        return blessing != null
                && blessing.unwrapKey()
                .map(key -> key.location())
                .filter(ARCHITECT_REACH::equals)
                .isPresent();
    }
}
