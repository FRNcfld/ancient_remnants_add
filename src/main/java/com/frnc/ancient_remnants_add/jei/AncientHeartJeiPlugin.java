package com.frnc.ancient_remnants_add.jei;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.frnc.ancient_remnants_add.altar.AltarRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * JEI 教程页：在 JEI 中查看远古之心或巨石典籍的物品信息，即可看到祭坛的完整流程。
 *
 * <p>JEI 是可选前置；本类只由 JEI 通过 {@link JeiPlugin} 发现，未安装 JEI 时不会被加载。
 */
@JeiPlugin
public final class AncientHeartJeiPlugin implements IModPlugin {

    private static final ResourceLocation UID =
            new ResourceLocation(AncientRemnantsAdd.MOD_ID, "ancient_heart_tutorial");
    private static final ResourceLocation CODEX_ID =
            new ResourceLocation(AncientRemnantsAdd.ANCIENT_REMNANTS_MOD_ID, "monolith_codex");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Component[] pages = new Component[]{
                Component.translatable("jei.ancient_remnants_add.altar.page1"),
                Component.translatable("jei.ancient_remnants_add.altar.page2"),
                Component.translatable("jei.ancient_remnants_add.altar.page3"),
                Component.translatable("jei.ancient_remnants_add.altar.page4"),
                Component.translatable("jei.ancient_remnants_add.altar.page5"),
                Component.translatable("jei.ancient_remnants_add.altar.page6"),
                Component.translatable("jei.ancient_remnants_add.altar.page7"),
                Component.translatable("jei.ancient_remnants_add.altar.page8")
        };

        registration.addItemStackInfo(
                new ItemStack(AltarRegistry.ANCIENT_HEART_ITEM.get()),
                pages
        );

        Item codex = ForgeRegistries.ITEMS.getValue(CODEX_ID);
        if (codex != null) {
            registration.addItemStackInfo(new ItemStack(codex), pages);
        }
    }
}
