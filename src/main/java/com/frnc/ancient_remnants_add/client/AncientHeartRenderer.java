package com.frnc.ancient_remnants_add.client;

import com.frnc.ancient_remnants_add.AncientRemnantsAdd;
import com.frnc.ancient_remnants_add.altar.AncientHeartBlock;
import com.frnc.ancient_remnants_add.altar.AncientHeartBlockEntity;
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
 * 远古之心激活后，在中央尖塔顶部上方悬浮渲染缩小版的「休眠核心」（上游 Elderheart 模型）。
 *
 * <p>只有 ROOT 持有 BlockEntity，因此核心只会从 ROOT 渲染一次；典籍不参与渲染。
 */
public class AncientHeartRenderer implements BlockEntityRenderer<AncientHeartBlockEntity> {

    private static final ResourceLocation ELDERHEART_TEXTURE =
            new ResourceLocation(AncientRemnantsAdd.ANCIENT_REMNANTS_MOD_ID, "textures/entity/elderheart.png");

    private final ModelElderheart coreModel;

    public AncientHeartRenderer(BlockEntityRendererProvider.Context context) {
        this.coreModel = new ModelElderheart(context.bakeLayer(ModelElderheart.Companion.getLAYER()));
    }

    @Override
    public void render(AncientHeartBlockEntity heart, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (heart.getBlockState().getValue(AncientHeartBlock.PART) != AncientHeartBlock.Part.ROOT
                || !heart.getBlockState().getValue(AncientHeartBlock.ACTIVE)) {
            return;
        }
        Level level = heart.getLevel();
        float time = level != null ? level.getGameTime() + partialTick : partialTick;

        poseStack.pushPose();
        // ROOT 在 2×2 的一角，中心在 +1.0 格处；中央尖塔顶面相对 ROOT 为 +22.0，
        // 核心再悬浮 0.45 格。
        poseStack.translate(1.0D, 22.45D + Math.sin(time * 0.05D) * 0.04D, 1.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * 1.2F));
        poseStack.scale(0.45F, 0.45F, 0.45F);
        VertexConsumer cutout = buffer.getBuffer(RenderType.entityCutoutNoCull(ELDERHEART_TEXTURE));
        coreModel.renderToBuffer(poseStack, cutout, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        VertexConsumer glow = buffer.getBuffer(RenderType.eyes(ELDERHEART_TEXTURE));
        coreModel.renderToBuffer(poseStack, glow, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
        poseStack.popPose();
    }

    /**
     * 核心在 ROOT 上方 22 格；提高可见距离，保证玩家站在祭坛/尖塔附近时能稳定看到核心。
     * 1.20.1 的 BlockEntityRenderer 没有 getRenderBoundingBox，距离由 shouldRender/getViewDistance 控制。
     */
    @Override
    public int getViewDistance() {
        return 96;
    }
}
