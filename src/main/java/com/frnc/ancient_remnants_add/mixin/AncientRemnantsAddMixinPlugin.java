package com.frnc.ancient_remnants_add.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Create 是软依赖，所以 create 子包下的 mixin 只有在检测到 Create 类存在时才应用。
 *
 * <p>没装 Create 时，这些 mixin 直接从目标集合里移除，不会因为目标类缺失而崩溃；
 * 其余（远古遗迹相关）mixin 不受影响。
 */
public final class AncientRemnantsAddMixinPlugin implements IMixinConfigPlugin {

    private static final String CREATE_MIXIN_PREFIX =
            "com.frnc.ancient_remnants_add.mixin.create.";

    private static final boolean CREATE_PRESENT = isClassPresent("com.simibubi.create.Create");

    private static boolean isClassPresent(String className) {
        ClassLoader[] loaders = {
                AncientRemnantsAddMixinPlugin.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader()
        };
        for (ClassLoader loader : loaders) {
            if (loader == null) {
                continue;
            }
            try {
                Class.forName(className, false, loader);
                return true;
            } catch (Throwable ignored) {
                // 尝试下一个 classloader
            }
        }
        return false;
    }

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith(CREATE_MIXIN_PREFIX)) {
            return CREATE_PRESENT;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
