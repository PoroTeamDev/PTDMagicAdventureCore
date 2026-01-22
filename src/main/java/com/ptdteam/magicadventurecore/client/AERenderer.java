package com.ptdteam.magicadventurecore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ptdteam.magicadventurecore.world.block.entity.AEBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AERenderer implements BlockEntityRenderer<AEBlockEntity> {
    private final ItemRenderer itemRenderer;

    public AERenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(
            @NotNull AEBlockEntity blockEntity,
            float partialTick,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int packedLight,
            int packedOverlay
    ) {
        ItemStack stack = blockEntity.getDisplayItem();
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        float time = (blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0) + partialTick;
        float bob = Mth.sin(time * 0.1F) * 0.05F;
        poseStack.translate(0.5F, 0.5F + bob, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees((time * 4F) % 360F));
        poseStack.scale(0.75F, 0.75F, 0.75F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, packedOverlay, poseStack, buffer,
                blockEntity.getLevel(), 0);
        poseStack.popPose();
    }
}