package com.ptdteam.magicadventurecore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ptdteam.magicadventurecore.world.block.entity.AEBlockEntity;
import com.ptdteam.magicadventurecore.world.particle.ArcaneCrossParticleData;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
        Level level = blockEntity.getLevel();
        float progress = blockEntity.getClientProgress();
        float time = (level != null ? level.getGameTime() : 0) + partialTick;

        poseStack.pushPose();
        float bob = Mth.sin(time * 0.1F) * 0.04F;
        poseStack.translate(0.5F, 0.5F + bob, 0.5F);
        // Скорость вращения растёт от 1.5°/тик в начале до ~31.5°/тик к концу переработки.
        float speedDegreesPerTick = 1.5F + 30.0F * progress;
        poseStack.mulPose(Axis.YP.rotationDegrees((time * speedDegreesPerTick) % 360F));
        poseStack.scale(0.75F, 0.75F, 0.75F);
        itemRenderer.renderStatic(stack, ItemDisplayContext.GROUND, packedLight, packedOverlay, poseStack, buffer,
                level, 0);
        poseStack.popPose();

        spawnPurpleCloud(blockEntity, level, progress);
    }

    /**
     * Облако фиолетовых «крестиков»: чем дальше переработка, тем плотнее поток.
     * Частицы летят к ближайшему резервуару эссенции (если он найден).
     */
    private void spawnPurpleCloud(AEBlockEntity blockEntity, Level level, float progress) {
        if (level == null || progress <= 0.02F) {
            return;
        }
        BlockPos linked = blockEntity.getLinkedReservoir();
        Vec3 target = linked != null
                ? new Vec3(linked.getX() + 0.5D, linked.getY() + 0.6D, linked.getZ() + 0.5D)
                : Vec3.ZERO;
        // 1..22 частиц за кадр: к концу переработки получается густое облако.
        int count = 1 + Mth.floor(progress * progress * 21.0F);
        double cx = blockEntity.getBlockPos().getX() + 0.5D;
        double cy = blockEntity.getBlockPos().getY() + 0.6D;
        double cz = blockEntity.getBlockPos().getZ() + 0.5D;
        for (int i = 0; i < count; i++) {
            double spread = 0.18D + 0.12D * progress;
            double x = cx + (level.random.nextDouble() - 0.5D) * spread * 2.0D;
            double y = cy + (level.random.nextDouble() - 0.5D) * spread;
            double z = cz + (level.random.nextDouble() - 0.5D) * spread * 2.0D;
            level.addParticle(new ArcaneCrossParticleData(target), x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }
}
