package com.frnc.ancient_remnants_add.client;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.frnc.ancient_remnants_add.altar.MonolithAltarBlock;
import com.frnc.ancient_remnants_add.altar.MonolithAltarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.obscuria.ancient_remnants.client.ModelElderheart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

/**
 * 祭坛激活后，在方块中央悬浮渲染缩小版的「休眠核心」（上游的 Elderheart 模型）。
 *
 * <p>典籍只保存在 BlockEntity 里供空手右键取回，不再参与渲染。
 */
public class MonolithAltarRenderer implements BlockEntityRenderer<MonolithAltarBlockEntity> {

    private static final ResourceLocation ELDERHEART_TEXTURE =
            new ResourceLocation(AncientRemnantsAdd.ANCIENT_REMNANTS_MOD_ID, "textures/entity/elderheart.png");

    private final ModelElderheart coreModel;

    public MonolithAltarRenderer(BlockEntityRendererProvider.Context context) {
        this.coreModel = new ModelElderheart(context.bakeLayer(ModelElderheart.Companion.getLAYER()));
    }

    @Override
    public void render(MonolithAltarBlockEntity altar, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (!altar.getBlockState().getValue(MonolithAltarBlock.ACTIVE)) {
            return;
        }
        Level level = altar.getLevel();
        float time = level != null ? level.getGameTime() + partialTick : partialTick;

        poseStack.pushPose();
        poseStack.translate(0.5D, 1.45D + Math.sin(time * 0.05D) * 0.04D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.2F));
        poseStack.scale(0.45F, 0.45F, 0.45F);
        VertexConsumer cutout = buffer.getBuffer(RenderType.entityCutoutNoCull(ELDERHEART_TEXTURE));
        coreModel.renderToBuffer(poseStack, cutout, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(ELDERHEART_TEXTURE));
        coreModel.renderToBuffer(poseStack, glow, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }
}
