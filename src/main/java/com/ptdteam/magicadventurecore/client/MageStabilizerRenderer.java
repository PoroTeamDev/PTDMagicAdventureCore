package com.ptdteam.magicadventurecore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ptdteam.magicadventurecore.world.block.entity.MageStabilizerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;

/**
 * Свечение кристаллов стабилизатора: чем больше заряд, тем ярче. Сама модель блока —
 * обычный JSON, а здесь рисуются аддитивные «искорки» вокруг кристаллов.
 */
public class MageStabilizerRenderer implements BlockEntityRenderer<MageStabilizerBlockEntity> {
    /** Точки свечения у кристаллов (в координатах блока 0..1). */
    private static final float[][] POINTS = {
            {0.50F, 0.42F, 0.50F},
            {0.44F, 0.60F, 0.54F},
            {0.56F, 0.68F, 0.46F},
            {0.50F, 0.82F, 0.50F},
            {0.60F, 0.50F, 0.58F}
    };
    private static final float RADIUS = 0.085F;

    public MageStabilizerRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(MageStabilizerBlockEntity blockEntity, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        float fraction = blockEntity.getChargeFraction();
        if (fraction <= 0.02F) {
            return;
        }
        float alpha = 0.04F + 0.30F * fraction;
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = pose.last().pose();
        for (float[] point : POINTS) {
            float x = point[0];
            float y = point[1];
            float z = point[2];
            // Два скрещённых вертикальных квада — «объёмное» свечение без текстуры.
            addQuad(consumer, matrix, x, y, z, RADIUS, 0.0F, 1.0F, alpha);
            addQuad(consumer, matrix, x, y, z, RADIUS, 1.0F, 0.0F, alpha);
        }
    }

    private static void addQuad(VertexConsumer consumer, Matrix4f matrix,
                                float x, float y, float z, float radius,
                                float axisX, float axisZ, float alpha) {
        float r = 0.66F;
        float g = 0.58F;
        float b = 1.0F;
        float dx = radius * axisX;
        float dz = radius * axisZ;
        consumer.vertex(matrix, x - dx, y - radius, z - dz).color(r, g, b, alpha).endVertex();
        consumer.vertex(matrix, x - dx, y + radius, z - dz).color(r, g, b, alpha).endVertex();
        consumer.vertex(matrix, x + dx, y + radius, z + dz).color(r, g, b, alpha).endVertex();
        consumer.vertex(matrix, x + dx, y - radius, z + dz).color(r, g, b, alpha).endVertex();
    }
}
