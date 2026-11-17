package com.ptdteam.magicadventurecore.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.world.block.entity.SCTBlockEntity;
import com.ptdteam.magicadventurecore.world.menu.SCTAnimationState;
import com.ptdteam.magicadventurecore.world.menu.SCTCraftingMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

public class SCTScreen extends AbstractContainerScreen<SCTCraftingMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MagicAdventureCore.MOD_ID, "textures/gui/sct_gui.png");
    /** Ванильная текстура частицы flash (32x32) — для всех партиклов стола. */
    private static final ResourceLocation PARTICLE_FLASH = ResourceLocation.fromNamespaceAndPath(
            "minecraft", "textures/particle/flash.png");

    /** Чередование оттенков для потока от шкалы крови: красный, яркий, тусклый, полупрозрачный. */
    private static final int[] BLOOD_PALETTE = {
            0xFFE01B1B, 0xFFFF4A4A, 0xFFB81414, 0xC4FF6B6B
    };
    /** То же для маны — оттенки синего. */
    private static final int[] MANA_PALETTE = {
            0xFF2E86DE, 0xFF66B8FF, 0xFF1F63B5, 0xC44FA8FF
    };

    // Старый стиль: вертикальные шкалы по бокам окна (кровь слева, мана справа).
    private static final int BAR_WIDTH = 8;
    private static final int BAR_HEIGHT = 60;
    private static final int BAR_Y = 45;
    private static final int BLOOD_BAR_OFFSET = -BAR_WIDTH - 4;
    private static final int MANA_BAR_OFFSET = 4;

    private static final int BLOOD_FILL = 0xFFB01212;
    private static final int BLOOD_EMPTY = 0xFF2C0606;
    private static final int MANA_FILL = 0xFF2E86DE;
    private static final int MANA_EMPTY = 0xFF071A2E;
    private static final int BAR_BORDER = 0xFF101010;

    // Шкала стабильности ритуала (в стиле полоски опыта) — под компонентами, над инвентарём.
    private static final int STABILITY_BAR_WIDTH = 100;
    private static final int STABILITY_BAR_HEIGHT = 4;
    private static final int STABILITY_BAR_Y = 132;
    private static final int STABILITY_FILL = 0xFFEDEAE0;
    private static final int STABILITY_EMPTY = 0xFF2B2B28;
    private static final int STABILITY_BORDER = 0xFF101010;

    private static final int MACHINE_SLOT_COUNT = SCTCraftingMenu.OUTPUT_SLOT + 1;
    /** Количество частиц в «туче» на каждую шкалу (первый вариант потока). */
    private static final int CLOUD_PARTICLES = 64;

    /** Базовые (неизменяемые) позиции слотов машины — запоминаем до начала анимации. */
    private final int[][] machineSlotPositions = new int[MACHINE_SLOT_COUNT][2];
    private int lastPhase = -1;
    private int lastAbsorbed = -1;

    public SCTScreen(SCTCraftingMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 230;
        for (int i = 0; i < MACHINE_SLOT_COUNT; i++) {
            machineSlotPositions[i][0] = menu.slots.get(i).x;
            machineSlotPositions[i][1] = menu.slots.get(i).y;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelY = 4;
        this.inventoryLabelY = 139;
    }

    /**
     * Подписи рисуем сами: в новом фоне нет ванильных полос под текст, а стандартный
     * тёмно-серый цвет на фиолетовом не читается — поэтому белым с тенью.
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleX = (this.imageWidth - this.font.width(this.title)) / 2;
        graphics.drawString(this.font, this.title, titleX, this.titleLabelY, 0xFFFFFF, true);
        graphics.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0xFFFFFF, true);
    }

    @Override
    public void removed() {
        super.removed();
        SCTAnimationState.reset();
    }

    // ------------------------------------------------------------------ шкалы

    private int bloodBarX() {
        return leftPos + BLOOD_BAR_OFFSET;
    }

    private int manaBarX() {
        return leftPos + imageWidth + MANA_BAR_OFFSET;
    }

    private int barY() {
        return topPos + BAR_Y;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);

        drawBar(graphics, bloodBarX(), barY(), menu.getBloodFill(), BLOOD_FILL, BLOOD_EMPTY);
        drawBar(graphics, manaBarX(), barY(), menu.getManaFill(), MANA_FILL, MANA_EMPTY);
        drawStabilityBar(graphics);
    }

    /** Шкала стабильности ритуала в стиле полоски опыта. */
    private void drawStabilityBar(GuiGraphics graphics) {
        int x = leftPos + (imageWidth - STABILITY_BAR_WIDTH) / 2;
        int y = topPos + STABILITY_BAR_Y;
        graphics.fill(x - 1, y - 1, x + STABILITY_BAR_WIDTH + 1, y + STABILITY_BAR_HEIGHT + 1,
                STABILITY_BORDER);
        graphics.fill(x, y, x + STABILITY_BAR_WIDTH, y + STABILITY_BAR_HEIGHT, STABILITY_EMPTY);
        int max = Math.max(1, menu.getStabilityMax());
        int filled = menu.getStability() * STABILITY_BAR_WIDTH / max;
        filled = Math.max(0, Math.min(STABILITY_BAR_WIDTH, filled));
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + STABILITY_BAR_HEIGHT, STABILITY_FILL);
            graphics.fill(x, y, x + filled, y + 1, 0x66FFFFFF);
        }
    }

    private void renderStabilityTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = leftPos + (imageWidth - STABILITY_BAR_WIDTH) / 2;
        int y = topPos + STABILITY_BAR_Y;
        if (mouseX >= x - 1 && mouseX < x + STABILITY_BAR_WIDTH + 1
                && mouseY >= y - 1 && mouseY < y + STABILITY_BAR_HEIGHT + 1) {
            int max = Math.max(1, menu.getStabilityMax());
            int percent = Math.min(100, menu.getStability() * 100 / max);
            graphics.renderTooltip(font,
                    Component.translatable("gui.magicadventurecore.stability", percent),
                    mouseX, mouseY);
        }
    }

    private void drawBar(GuiGraphics graphics, int x, int y, int percent, int fillColor, int emptyColor) {
        graphics.fill(x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 1, BAR_BORDER);
        graphics.fill(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, emptyColor);

        int clamped = Math.max(0, Math.min(100, percent));
        // Вертикальная шкала: заполняется снизу вверх.
        int filled = clamped * BAR_HEIGHT / 100;
        if (filled <= 0) {
            return;
        }
        int fillTop = y + BAR_HEIGHT - filled;
        graphics.fill(x, fillTop, x + BAR_WIDTH, y + BAR_HEIGHT, fillColor);
        graphics.fill(x, fillTop, x + BAR_WIDTH, fillTop + 1, 0x80FFFFFF);
    }

    // -------------------------------------------------------------- анимация

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        syncAnimationState(partialTick);
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderStatusIcons(graphics, mouseX, mouseY);
        renderStabilityTooltip(graphics, mouseX, mouseY);
        renderTooltip(graphics, mouseX, mouseY);
        renderBarTooltips(graphics, mouseX, mouseY);
        renderCraft(graphics, partialTick);
    }

    /**
     * Индикаторы под заголовком: платформа 3×3 и круг мела.
     * Наведение показывает подробности (число модулей / закрашенных клеток).
     */
    private void renderStatusIcons(GuiGraphics graphics, int mouseX, int mouseY) {
        int iconSize = 8;
        int gap = 6;
        int totalWidth = iconSize * 3 + gap * 2;
        int startX = leftPos + (imageWidth - totalWidth) / 2;
        int y = topPos + 13;

        // 1) Платформа: сетка 3×3.
        int structureX = startX;
        boolean formed = menu.isStructureFormed();
        int structureColor = formed ? 0xFF5CD65C : 0xFFD64B4B;
        graphics.fill(structureX - 2, y - 2, structureX + iconSize + 2, y + iconSize + 2, 0x90000000);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                graphics.fill(structureX + col * 3, y + row * 3,
                        structureX + col * 3 + 2, y + row * 3 + 2, structureColor);
            }
        }
        if (isOverIcon(mouseX, mouseY, structureX, y, iconSize)) {
            Component status = Component.translatable(
                    "gui.magicadventurecore.status.structure",
                    Component.translatable(formed
                            ? "gui.magicadventurecore.status.structure.formed"
                            : "gui.magicadventurecore.status.structure.missing"));
            if (formed) {
                status = status.copy().append("\n").append(Component.translatable(
                        "gui.magicadventurecore.status.modules", menu.getModuleCount()));
            }
            graphics.renderTooltip(font, status, mouseX, mouseY);
        }

        // 2) Круг мела: кольцо, цвет по прогрессу.
        int circleX = startX + iconSize + gap;
        int marks = menu.getCircleMarks();
        int required = menu.getRequiredCircleMarks();
        int circleColor = required > 0 && marks >= required
                ? 0xFF5CD65C
                : (marks > 0 ? 0xFFFFC94D : 0xFFD64B4B);
        graphics.fill(circleX - 2, y - 2, circleX + iconSize + 2, y + iconSize + 2, 0x90000000);
        drawRing(graphics, circleX + 4, y + 4, 4, 2, circleColor);
        if (isOverIcon(mouseX, mouseY, circleX, y, iconSize)) {
            graphics.renderTooltip(font, Component.translatable(
                    "gui.magicadventurecore.status.circle", marks, required), mouseX, mouseY);
        }

        // 3) Стабилизаторы: ромб, цвет по количеству и заряду.
        int stabilizerX = circleX + iconSize + gap;
        int stabilizerCount = menu.getStabilizerCount();
        int stabilizerCharge = menu.getStabilizerCharge();
        int stabilizerColor = stabilizerCount >= 4
                ? (stabilizerCharge > 0 ? 0xFF5CD65C : 0xFFFFC94D)
                : 0xFFD64B4B;
        graphics.fill(stabilizerX - 2, y - 2, stabilizerX + iconSize + 2, y + iconSize + 2, 0x90000000);
        drawDiamond(graphics, stabilizerX + 4, y + 4, 4, stabilizerColor);
        if (isOverIcon(mouseX, mouseY, stabilizerX, y, iconSize)) {
            graphics.renderTooltip(font, Component.translatable(
                    "gui.magicadventurecore.status.stabilizers",
                    stabilizerCount, stabilizerCharge, menu.getStabilizerChargeMax()),
                    mouseX, mouseY);
        }
    }

    /** Ромб из горизонтальных полос (иконка стабилизатора). */
    private static void drawDiamond(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = radius - Math.abs(dy);
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    /** Кольцо из горизонтальных полос (для иконки круга). */
    private static void drawRing(GuiGraphics graphics, int cx, int cy, int outer, int inner, int color) {
        for (int dy = -outer; dy <= outer; dy++) {
            int outerHalf = (int) Math.sqrt((double) (outer * outer - dy * dy));
            if (Math.abs(dy) >= inner) {
                graphics.fill(cx - outerHalf, cy + dy, cx + outerHalf + 1, cy + dy + 1, color);
                continue;
            }
            int innerHalf = (int) Math.sqrt((double) (inner * inner - dy * dy));
            graphics.fill(cx - outerHalf, cy + dy, cx - innerHalf, cy + dy + 1, color);
            graphics.fill(cx + innerHalf, cy + dy, cx + outerHalf + 1, cy + dy + 1, color);
        }
    }

    private static boolean isOverIcon(int mouseX, int mouseY, int x, int y, int size) {
        return mouseX >= x - 2 && mouseX < x + size + 2 && mouseY >= y - 2 && mouseY < y + size + 2;
    }

    private void syncAnimationState(float partialTick) {
        SCTBlockEntity blockEntity = menu.getBlockEntity();
        Level level = blockEntity != null ? blockEntity.getLevel() : null;
        if (blockEntity == null || level == null || !blockEntity.isCrafting()) {
            SCTAnimationState.reset();
            lastPhase = -1;
            lastAbsorbed = -1;
            return;
        }
        SCTAnimationState.update(
                blockEntity.getCraftStartGameTime(),
                blockEntity.getCraftResourceTicks(),
                blockEntity.getCraftDuration()
        );
        // Слоты прячутся от ванильного рендера только во время фазы поглощения.
        boolean resourcePhase = SCTAnimationState.inResourcePhase(level.getGameTime(), partialTick);
        SCTAnimationState.setItemPhase(!resourcePhase);
    }

    private void renderCraft(GuiGraphics graphics, float partialTick) {
        if (!SCTAnimationState.isActive()) {
            return;
        }
        SCTBlockEntity blockEntity = menu.getBlockEntity();
        if (blockEntity == null || blockEntity.getLevel() == null) {
            return;
        }
        long gameTime = blockEntity.getLevel().getGameTime();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        if (SCTAnimationState.inResourcePhase(gameTime, partialTick)) {
            float resourceProgress = SCTAnimationState.resourceProgress(gameTime, partialTick);
            renderResourceFlow(graphics, resourceProgress);
            updateSounds(-1, resourceProgress, 0F);
        } else {
            float progress = SCTAnimationState.itemProgress(gameTime, partialTick);
            renderItemPhase(graphics, blockEntity, progress, gameTime + partialTick);
            updateSounds(SCTAnimationState.itemPhaseKind(progress), 1F, progress);
        }
        RenderSystem.disableBlend();
    }

    /**
     * Фаза перекачки ресурсов: густая «туча» частиц поднимается со всей поверхности
     * шкалы крови (слева) и маны (справа) и медленно плывёт к центру интерфейса.
     * Скорость перекачки задана сервером: 1000 единиц в секунду.
     */
    private void renderResourceFlow(GuiGraphics graphics, float progress) {
        int centerX = machineCenterX(SCTCraftingMenu.OUTPUT_SLOT);
        int centerY = machineCenterY(SCTCraftingMenu.OUTPUT_SLOT);

        // Аддитивное смешивание: частицы светятся, а не выглядят как полупрозрачные квадраты.
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        drawCloud(graphics, bloodBarX(), barY(), progress, 0.0D, BLOOD_PALETTE, centerX, centerY);
        drawCloud(graphics, manaBarX(), barY(), progress, 0.71D, MANA_PALETTE, centerX, centerY);

        // Растущее свечение в центре.
        int glowSize = (int) (16 + 20 * progress);
        int glowAlpha = (int) (70 + 150 * progress);
        drawFlash(graphics, centerX, centerY, withAlpha(0xFFF2C14E, glowAlpha), glowSize);
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Первый вариант потока: густая туча частиц со всей площади шкалы, медленно
     * дрейфующая к центру. Количество фиксированное (CLOUD_PARTICLES на шкалу).
     */
    private void drawCloud(
            GuiGraphics graphics,
            int barLeft,
            int barTop,
            float progress,
            double seed,
            int[] palette,
            int centerX,
            int centerY
    ) {
        for (int i = 0; i < CLOUD_PARTICLES; i++) {
            double r1 = random01(i + 1, seed + 0.13D);
            double r2 = random01(i + 1, seed + 1.77D);
            double r3 = random01(i + 1, seed + 3.31D);
            // Партиклы стартуют со всей поверхности вертикальной шкалы.
            double startX = barLeft + 1 + r1 * (BAR_WIDTH - 2);
            double startY = barTop + r2 * BAR_HEIGHT;
            // Медленный дрейф: за всю фазу частица проходит меньше одного круга.
            double t = frac(r3 + progress * 0.75D);
            double eased = smoothstep(t);
            double wobble = Math.sin(t * 8.0D + i) * 5.0D * (1.0D - t);
            int x = (int) Mth.lerp(eased, startX, (double) centerX) + (int) wobble;
            int y = (int) Mth.lerp(eased, startY, (double) centerY) + (int) (wobble * 0.7D);
            // Чередуем оттенки: обычный, яркий, тусклый, полупрозрачный.
            int base = palette[(i + (int) (seed * 7.0D)) % palette.length];
            double envelope = 0.35D + 0.65D * Math.sin(Math.PI * Math.min(1.0D, t * 1.05D));
            int alpha = (int) (((base >>> 24) & 0xFF) * envelope);
            int size = 10 + (i % 4);
            drawFlash(graphics, x, y, withAlpha(base, alpha), size);
        }
    }

    /** Детерминированное псевдослучайное число 0..1 из номера частицы и сдвига. */
    private static double random01(int index, double seed) {
        return frac(Math.sin(index * 12.9898D + seed * 78.233D) * 43758.5453D);
    }

    private static double frac(double value) {
        return value - Math.floor(value);
    }

    private void renderItemPhase(GuiGraphics graphics, SCTBlockEntity blockEntity, float progress, double seconds) {
        ItemStackHandler inventory = blockEntity.getInventory();
        double amplitude = jitterAmplitude(progress);
        for (int i = 0; i < SCTCraftingMenu.INPUT_SLOTS; i++) {
            renderInputSlot(graphics, inventory, i, progress, seconds, amplitude);
        }
        // Слоты сферы и мана-предмета не поглощаются — только трясутся.
        for (int i = SCTCraftingMenu.BLOOD_SLOT; i <= SCTCraftingMenu.MANA_SLOT; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            drawJitteredItem(graphics, stack, i, seconds, amplitude * 0.5D);
        }

        int centerX = machineCenterX(SCTCraftingMenu.OUTPUT_SLOT);
        int centerY = machineCenterY(SCTCraftingMenu.OUTPUT_SLOT);
        int phase = SCTAnimationState.itemPhaseKind(progress);
        if (phase == 1) {
            renderChargingParticles(graphics, inventory, progress, centerX, centerY);
        } else if (phase == 3) {
            renderFinish(graphics, inventory.getStackInSlot(SCTCraftingMenu.OUTPUT_SLOT), progress, centerX, centerY);
        }
    }

    private void renderInputSlot(
            GuiGraphics graphics,
            ItemStackHandler inventory,
            int slot,
            float progress,
            double seconds,
            double amplitude
    ) {
        ItemStack stack = inventory.getStackInSlot(slot);
        if (stack.isEmpty()) {
            return;
        }
        double absorbAt = SCTAnimationState.absorbStart(slot);
        if (progress < absorbAt) {
            drawJitteredItem(graphics, stack, slot, seconds, amplitude);
            return;
        }
        double flight = (progress - absorbAt) / SCTAnimationState.FLIGHT;
        if (flight >= 1.0D) {
            return;
        }
        int startX = machineCenterX(slot);
        int startY = machineCenterY(slot);
        int centerX = machineCenterX(SCTCraftingMenu.OUTPUT_SLOT);
        int centerY = machineCenterY(SCTCraftingMenu.OUTPUT_SLOT);
        int color = colorFor(stack);
        // Почти невидимый шлейф: процедурные мягкие точки (не зависят от фильтра текстур).
        for (int trail = 1; trail <= 3; trail++) {
            double local = clamp01(flight - trail * 0.10D);
            int x = (int) Mth.lerp(smoothstep(local), startX, centerX);
            int y = (int) Mth.lerp(smoothstep(local), startY, centerY);
            double fade = Math.sin(Math.PI * clamp01(0.35D + local * 0.65D));
            int alpha = (int) (55 * fade * (1.0D - trail / 4.0D));
            drawSoftDot(graphics, x, y, color, 2 + (trail == 1 ? 1 : 0), alpha);
        }
        int x = (int) Mth.lerp(smoothstep(flight), startX, centerX);
        int y = (int) Mth.lerp(smoothstep(flight), startY, centerY);
        float scale = (float) (1.0D - 0.6D * flight);
        drawScaledItem(graphics, stack, x, y, scale);
    }

    private void drawJitteredItem(GuiGraphics graphics, ItemStack stack, int slot, double seconds, double amplitude) {
        // Частота выше, амплитуда ниже — мелкая быстрая дрожь вместо сильного качания.
        int offsetX = (int) Math.round(Math.sin(seconds * 2.7D + slot * 1.7D) * amplitude);
        int offsetY = (int) Math.round(Math.cos(seconds * 3.3D + slot * 0.9D) * amplitude);
        drawScaledItem(graphics, stack, machineCenterX(slot) + offsetX, machineCenterY(slot) + offsetY, 1.0F);
    }

    /** Раскрутка: мелкие тусклые процедурные искры от каждого слота к центру. */
    private void renderChargingParticles(GuiGraphics graphics, ItemStackHandler inventory, float progress, int centerX, int centerY) {
        double t = progress / SCTAnimationState.CHARGE_END;
        for (int i = 0; i < SCTCraftingMenu.INPUT_SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }
            int color = colorFor(stack);
            int startX = machineCenterX(i);
            int startY = machineCenterY(i);
            for (int spark = 0; spark < 3; spark++) {
                double local = clamp01(t - spark * 0.12D);
                if (local <= 0.0D) {
                    continue;
                }
                double wobble = Math.sin(local * 10.0D + i + spark) * 6.0D * (1.0D - local);
                int x = (int) Mth.lerp(local, startX, centerX) + (int) (wobble * (1.0D - local));
                int y = (int) Mth.lerp(local, startY, centerY) + (int) (wobble * (1.0D - local));
                double fade = Math.sin(Math.PI * local);
                drawSoftDot(graphics, x, y, color, 3, (int) (80 * fade));
            }
        }
    }

    /** Финал: как в первом варианте — стопка градиентных прямоугольников + белая вспышка. */
    private void renderFinish(GuiGraphics graphics, ItemStack result, float progress, int centerX, int centerY) {
        float f = clamp01((progress - SCTAnimationState.FINISH_START) / (1.0F - SCTAnimationState.FINISH_START));
        int glowColor = result.isEmpty() ? 0xFFF2C14E : colorFor(result);
        for (int layer = 10; layer >= 1; layer--) {
            int radius = 3 + layer * 3;
            int alpha = (int) (70 * (1.0F - layer / 11.0F) * (0.4F + 0.6F * f));
            graphics.fill(centerX - radius, centerY - radius, centerX + radius, centerY + radius,
                    withAlpha(glowColor, alpha));
        }
        if (f < 0.3F) {
            int flashAlpha = (int) (200 * (1.0F - f / 0.3F));
            graphics.fill(centerX - 14, centerY - 14, centerX + 14, centerY + 14,
                    (flashAlpha << 24) | 0xFFFFFF);
        }
        if (!result.isEmpty()) {
            float pulse = 1.0F + 0.25F * Mth.sin(f * (float) Math.PI);
            drawScaledItem(graphics, result, centerX, centerY, pulse);
        }
    }

    private double jitterAmplitude(float progress) {
        if (progress < SCTAnimationState.CHARGE_END) {
            return 0.8D * (progress / SCTAnimationState.CHARGE_END);
        }
        if (progress < SCTAnimationState.FINISH_START) {
            return 0.8D;
        }
        return 0.8D * Math.max(0.0D, (1.0D - progress) / (1.0D - SCTAnimationState.FINISH_START));
    }

    /**
     * @param phase -1 — фаза перекачки ресурсов, 1 — раскрутка, 2 — поглощение, 3 — финал
     */
    private void updateSounds(int phase, float resourceProgress, float itemProgress) {
        if (phase != lastPhase) {
            if (phase == -1) {
                playUi(SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F);
            } else if (phase == 1) {
                playUi(SoundEvents.AMETHYST_BLOCK_CHIME, 1.3F);
            } else if (phase == 3) {
                playUi(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F);
            }
            lastPhase = phase;
            lastAbsorbed = -1;
        }
        if (phase == 2) {
            int absorbed = 0;
            for (int i = 0; i < SCTCraftingMenu.INPUT_SLOTS; i++) {
                if (itemProgress >= SCTAnimationState.absorbStart(i)) {
                    absorbed++;
                }
            }
            if (absorbed > lastAbsorbed) {
                if (lastAbsorbed < 0 || absorbed - lastAbsorbed > 1) {
                    playUi(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.6F);
                }
                lastAbsorbed = absorbed;
            }
        }
    }

    private static void playUi(SoundEvent event, float pitch) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event, 1.0F, pitch));
    }

    // ------------------------------------------------------------ координаты

    private int machineCenterX(int slot) {
        return leftPos + machineSlotPositions[slot][0] + 8;
    }

    private int machineCenterY(int slot) {
        return topPos + machineSlotPositions[slot][1] + 8;
    }

    private void drawScaledItem(GuiGraphics graphics, ItemStack stack, int centerX, int centerY, float scale) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(centerX, centerY, 120.0F);
        pose.scale(scale, scale, 1.0F);
        graphics.renderItem(stack, -8, -8);
        pose.popPose();
    }

    /**
     * Мягкая точка из трёх вложенных кругов с убывающей альфой.
     * Не зависит от фильтрации текстур и гарантированно даёт затухание к краю.
     */
    private static void drawSoftDot(GuiGraphics graphics, int cx, int cy, int color, int radius, int alpha) {
        if (alpha <= 3) {
            return;
        }
        drawCircle(graphics, cx, cy, Math.max(1, radius), withAlpha(color, alpha / 4));
        drawCircle(graphics, cx, cy, Math.max(1, radius * 2 / 3), withAlpha(color, alpha / 2));
        drawCircle(graphics, cx, cy, Math.max(1, radius / 2), withAlpha(color, alpha));
    }

    private static void drawCircle(GuiGraphics graphics, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.sqrt((double) (radius * radius - dy * dy));
            graphics.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }

    /**
     * Рисует ванильный flash.png прямым квадом с явными UV (0..1) в мировых координатах GUI.
     * Через {@code GuiGraphics.blit} с textureWidth/Height легко попасть на неверный overload,
     * а спрайт из particle-атласа недоступен через Minecraft#getTextureAtlas (атлас не в AtlasSet).
     */
    private static void drawFlash(GuiGraphics graphics, int x, int y, int color, int size) {
        drawQuad(graphics, PARTICLE_FLASH, x, y, color, size);
    }

    /** Рисует текстуру квадом с тонированием ARGB и явными UV (0..1). */
    private static void drawQuad(GuiGraphics graphics, ResourceLocation texture, int x, int y, int color, int size) {
        int alpha = (color >>> 24) & 0xFF;
        if (alpha <= 4) {
            return;
        }
        float a = alpha / 255.0F;
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        int half = size / 2;
        float x0 = x - half;
        float y0 = y - half;
        float x1 = x0 + size;
        float y1 = y0 + size;

        Matrix4f matrix = graphics.pose().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(r, g, b, a);
        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.vertex(matrix, x0, y1, 0.0F).uv(0.0F, 1.0F).endVertex();
        buffer.vertex(matrix, x1, y1, 0.0F).uv(1.0F, 1.0F).endVertex();
        buffer.vertex(matrix, x1, y0, 0.0F).uv(1.0F, 0.0F).endVertex();
        buffer.vertex(matrix, x0, y0, 0.0F).uv(0.0F, 0.0F).endVertex();
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static int colorFor(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0xFFBFC4CC;
        }
        Integer rgb = stack.getRarity().color.getColor();
        return rgb == null ? 0xFFBFC4CC : (0xFF000000 | rgb);
    }

    private static int withAlpha(int color, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (color & 0xFFFFFF);
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static double smoothstep(double value) {
        double t = clamp01(value);
        return t * t * (3.0D - 2.0D * t);
    }

    // ------------------------------------------------------------ тултипы

    private void renderBarTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = barY();
        if (isOverBar(mouseX, mouseY, bloodBarX(), y)) {
            graphics.renderTooltip(font, Component.translatable(
                    "gui.magicadventurecore.blood",
                    menu.getBloodAmount(), menu.getBloodCapacity()), mouseX, mouseY);
            return;
        }
        if (isOverBar(mouseX, mouseY, manaBarX(), y)) {
            graphics.renderTooltip(font, Component.translatable(
                    "gui.magicadventurecore.mana",
                    menu.getManaAmount(), menu.getManaCapacity()), mouseX, mouseY);
        }
    }

    private static boolean isOverBar(int mouseX, int mouseY, int barX, int barY) {
        return mouseX >= barX && mouseX < barX + BAR_WIDTH
                && mouseY >= barY && mouseY < barY + BAR_HEIGHT;
    }
}
