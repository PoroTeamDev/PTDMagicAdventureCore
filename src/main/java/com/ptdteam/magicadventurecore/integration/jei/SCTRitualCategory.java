package com.ptdteam.magicadventurecore.integration.jei;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.registry.MACItems;
import com.ptdteam.magicadventurecore.world.ritual.ChalkCircle;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Отдельная JEI-категория «Ритуал SCT»: схема площадки 13×13 сверху — где стоит стол,
 * где стабилизаторы (±6, 0)/(0, ±6) и какие клетки нужно закрасить мелом (88 клеток),
 * плюс слоты-легенда с предметами ритуала.
 */
public class SCTRitualCategory implements IRecipeCategory<SCTRitualCategory.RitualInfo> {
    public static final RecipeType<RitualInfo> TYPE =
            RecipeType.create(MagicAdventureCore.MOD_ID, "sct_ritual", RitualInfo.class);

    /** Одна информационная запись — схема ритуала. */
    public record RitualInfo() {
        public static final RitualInfo INSTANCE = new RitualInfo();
    }

    private static final int WIDTH = 186;
    private static final int HEIGHT = 196;
    private static final int CELL = 9;
    private static final int GRID_X = 8;
    private static final int GRID_Y = 12;
    private static final int LEGEND_X = 138;
    private static final int LEGEND_Y = 12;

    private static final int COLOR_BACKGROUND = 0xFF3A3A44;
    private static final int COLOR_PLATFORM = 0xFF7A4FBF;
    private static final int COLOR_CHALK = 0xFFD6C8FF;
    private static final int COLOR_STABILIZER = 0xFF2E86DE;
    private static final int COLOR_TEXT = 0xFF222222;

    private final IDrawable background;
    private final IDrawable icon;

    public SCTRitualCategory(IGuiHelper guiHelper) {
        this.background = new PanelBackground();
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(MACItems.ARCANE_CHALK.get()));
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public RecipeType<RitualInfo> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gui.magicadventurecore.jei.ritual_title");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RitualInfo recipe, IFocusGroup focuses) {
        // Легенда: предметы, которые нужны для ритуала.
        addLegendSlot(builder, 0, new ItemStack(MACItems.SCT.get()));
        addLegendSlot(builder, 1, new ItemStack(MACItems.ARCANE_CHALK.get()));
        addLegendSlot(builder, 2, new ItemStack(MACItems.MAGE_STABILIZER.get()));
        addLegendSlot(builder, 3, new ItemStack(MACItems.SCT_FRAME.get()));
        addLegendSlot(builder, 4, new ItemStack(MACItems.SCT_MANA_RESERVOIR.get()));
        addLegendSlot(builder, 5, new ItemStack(MACItems.SCT_BLOOD_RESERVOIR.get()));
        addLegendSlot(builder, 6, new ItemStack(MACItems.SCT_ACCELERATOR.get()));
    }

    private static void addLegendSlot(IRecipeLayoutBuilder builder, int index, ItemStack stack) {
        builder.addSlot(RecipeIngredientRole.INPUT,
                        LEGEND_X, LEGEND_Y + index * 20)
                .addItemStack(stack);
    }

    @Override
    public void draw(RitualInfo recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics,
                     double mouseX, double mouseY) {
        Minecraft minecraft = Minecraft.getInstance();

        // Схема 13×13: платформа 3×3 в центре, кольцо мела, стабилизаторы по сторонам.
        for (int dz = -6; dz <= 6; dz++) {
            for (int dx = -6; dx <= 6; dx++) {
                int x = GRID_X + (dx + 6) * CELL;
                int y = GRID_Y + (dz + 6) * CELL;
                int color = COLOR_BACKGROUND;
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    color = COLOR_PLATFORM;
                } else if (isStabilizer(dx, dz)) {
                    color = COLOR_STABILIZER;
                } else if (isChalk(dx, dz)) {
                    color = COLOR_CHALK;
                }
                graphics.fill(x, y, x + CELL - 1, y + CELL - 1, color);
            }
        }

        int textY = GRID_Y + 13 * CELL + 6;
        graphics.drawString(minecraft.font,
                Component.translatable("gui.magicadventurecore.jei.ritual.circle", ChalkCircle.REQUIRED_MARKS),
                GRID_X, textY, COLOR_TEXT, false);
        graphics.drawString(minecraft.font,
                Component.translatable("gui.magicadventurecore.jei.ritual.ring"),
                GRID_X, textY + 12, COLOR_TEXT, false);
        graphics.drawString(minecraft.font,
                Component.translatable("gui.magicadventurecore.jei.ritual.stabilizers"),
                GRID_X, textY + 24, COLOR_TEXT, false);
        graphics.drawString(minecraft.font,
                Component.translatable("gui.magicadventurecore.jei.ritual.charge"),
                GRID_X, textY + 36, COLOR_TEXT, false);
    }

    private static boolean isStabilizer(int dx, int dz) {
        return (Math.abs(dx) == 6 && dz == 0) || (Math.abs(dz) == 6 && dx == 0);
    }

    private static boolean isChalk(int dx, int dz) {
        if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
            return false;
        }
        return dx * dx + dz * dz <= 30;
    }

    /** Фон — панель в стиле ванильных контейнеров. */
    private static final class PanelBackground implements IDrawable {
        @Override
        public int getWidth() {
            return WIDTH;
        }

        @Override
        public int getHeight() {
            return HEIGHT;
        }

        @Override
        public void draw(GuiGraphics graphics, int xOffset, int yOffset) {
            graphics.fill(xOffset, yOffset, xOffset + WIDTH, yOffset + HEIGHT, 0xFFC6C6C6);
            graphics.fill(xOffset, yOffset, xOffset + WIDTH, yOffset + 1, 0xFFFFFFFF);
            graphics.fill(xOffset, yOffset, xOffset + 1, yOffset + HEIGHT, 0xFFFFFFFF);
            graphics.fill(xOffset, yOffset + HEIGHT - 1, xOffset + WIDTH, yOffset + HEIGHT, 0xFF555555);
            graphics.fill(xOffset + WIDTH - 1, yOffset, xOffset + WIDTH, yOffset + HEIGHT, 0xFF555555);
            // Рамки под слоты легенды.
            for (int index = 0; index < 7; index++) {
                int x = xOffset + LEGEND_X - 1;
                int y = yOffset + LEGEND_Y + index * 20 - 1;
                graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
                graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF373737);
            }
        }
    }
}
