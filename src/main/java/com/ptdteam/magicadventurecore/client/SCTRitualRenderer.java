package com.ptdteam.magicadventurecore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.world.block.entity.SCTBlockEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Рисунок магического круга над площадкой SCT.
 *
 * <p>Пока круг не начерчен — рисунок почти прозрачный, по мере закрашивания клеток
 * проявляется. Когда все 88 клеток отмечены, круг становится ярким и медленно вращается.
 * Текстура 13×13 блоков с прозрачным центром 3×3 (там стоит сама платформа).</p>
 */
public class SCTRitualRenderer implements BlockEntityRenderer<SCTBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MagicAdventureCore.MOD_ID, "textures/block/ritual_circle.png");
    /** Половина стороны рисунка в блоках (13×13). */
    private static final float HALF = 6.5F;
    private static final float HEIGHT = 0.03F;

    public SCTRitualRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SCTBlockEntity blockEntity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        int marks = blockEntity.getCircleMarks();
        int required = Math.max(1, blockEntity.getRequiredCircleMarks());
        if (marks <= 0) {
            return;
        }
        float progress = Math.min(1.0F, marks / (float) required);
        boolean complete = marks >= required;
        // Незаконченный круг — бледный, законченный — яркий.
        float alpha = complete ? 0.85F : 0.08F + 0.37F * progress;
        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;
        float rotation = ((gameTime + partialTick) * 0.35F) % 360.0F;

        pose.pushPose();
        pose.translate(0.5D, HEIGHT, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        Matrix4f matrix = pose.last().pose();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucent(TEXTURE));
        // Обход вершин против часовой стрелки (нормаль вверх), иначе quad отсекается снизу.
        addVertex(consumer, matrix, -HALF, -HALF, 0.0F, 1.0F, alpha);
        addVertex(consumer, matrix, -HALF, HALF, 0.0F, 0.0F, alpha);
        addVertex(consumer, matrix, HALF, HALF, 1.0F, 0.0F, alpha);
        addVertex(consumer, matrix, HALF, -HALF, 1.0F, 1.0F, alpha);
        pose.popPose();
    }

    private static void addVertex(VertexConsumer consumer, Matrix4f matrix,
                                  float x, float z, float u, float v, float alpha) {
        consumer.vertex(matrix, x, 0.0F, z)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT)
                .normal(0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
