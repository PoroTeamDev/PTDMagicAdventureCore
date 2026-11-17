package com.ptdteam.magicadventurecore.integration.jei;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.registry.MACItems;
import com.ptdteam.magicadventurecore.world.menu.SCTCraftingMenu;
import com.ptdteam.magicadventurecore.world.recipe.SCTHybridRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public class SCTJeiCategory implements IRecipeCategory<SCTHybridRecipe> {
    public static final RecipeType<SCTHybridRecipe> TYPE =
            RecipeType.create(MagicAdventureCore.MOD_ID, "sct_hybrid_crafting", SCTHybridRecipe.class);

    private static final int WIDTH = 190;
    private static final int HEIGHT = 178;
    /** Сдвиг сетки влево, чтобы справа поместились стрелка и слот результата. */
    private static final int INPUT_SHIFT_X = -26;
    private static final int OUTPUT_SLOT_X = 158;
    private static final int OUTPUT_SLOT_Y = 74;
    private static final int ARROW_X = 130;
    private static final int ARROW_Y = 78;
    private static final int LABEL_X = 6;
    private static final int MANA_LABEL_Y = 150;
    private static final int BLOOD_LABEL_Y = 162;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public SCTJeiCategory(IGuiHelper guiHelper) {
        this.background = new CategoryBackground(guiHelper);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(MACItems.SCT.get()));
        // Анимированная стрелка, показывающая, что получается в результате.
        this.arrow = guiHelper.createAnimatedRecipeArrow(120);
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public RecipeType<SCTHybridRecipe> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.magicadventurecore.sct_crafting");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SCTHybridRecipe recipe, IFocusGroup focuses) {
        List<Ingredient> ingredients = recipe.getIngredients();
        for (int index = 0; index < SCTCraftingMenu.INPUT_SLOTS; index++) {
            Ingredient ingredient = ingredients.size() > index ? ingredients.get(index) : Ingredient.EMPTY;
            int x = SCTCraftingMenu.INPUT_SLOT_POSITIONS[index][0] + INPUT_SHIFT_X;
            int y = SCTCraftingMenu.INPUT_SLOT_POSITIONS[index][1];
            builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addIngredients(ingredient);
        }
        // Слоты кровяного орба и мана-предмета намеренно не показываем: источник
        // ресурсов понятен по подписям, а сами предметы зависят от игрока.

        ItemStack result = recipe.getResultItem(RegistryAccess.EMPTY);
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y)
                .addItemStack(result);
    }

    @Override
    public void draw(
            SCTHybridRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics graphics,
            double mouseX,
            double mouseY
    ) {
        arrow.draw(graphics, ARROW_X, ARROW_Y);

        Minecraft minecraft = Minecraft.getInstance();
        if (recipe.getManaCost() > 0) {
            graphics.drawString(minecraft.font, Component.translatable(
                    "gui.magicadventurecore.jei.mana_required", recipe.getManaCost()),
                    LABEL_X, MANA_LABEL_Y, 0x3F3F3F, false);
        }
        if (recipe.getBloodCost() > 0) {
            graphics.drawString(minecraft.font, Component.translatable(
                    "gui.magicadventurecore.jei.blood_required", recipe.getBloodCost()),
                    LABEL_X, BLOOD_LABEL_Y, 0x3F3F3F, false);
        }
    }

    /** Фон: панель + рамки слотов, чтобы предметы не висели в воздухе. */
    private static final class CategoryBackground implements IDrawable {
        private final IDrawable slotFrame;
        private final IDrawable outputFrame;

        private CategoryBackground(IGuiHelper guiHelper) {
            this.slotFrame = guiHelper.getSlotDrawable();
            this.outputFrame = guiHelper.getOutputSlot();
        }

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
            // Панель в стиле ванильных контейнеров.
            graphics.fill(xOffset, yOffset, xOffset + WIDTH, yOffset + HEIGHT, 0xFFC6C6C6);
            graphics.fill(xOffset, yOffset, xOffset + WIDTH, yOffset + 1, 0xFFFFFFFF);
            graphics.fill(xOffset, yOffset, xOffset + 1, yOffset + HEIGHT, 0xFFFFFFFF);
            graphics.fill(xOffset, yOffset + HEIGHT - 1, xOffset + WIDTH, yOffset + HEIGHT, 0xFF555555);
            graphics.fill(xOffset + WIDTH - 1, yOffset, xOffset + WIDTH, yOffset + HEIGHT, 0xFF555555);

            for (int index = 0; index < SCTCraftingMenu.INPUT_SLOTS; index++) {
                int x = xOffset + SCTCraftingMenu.INPUT_SLOT_POSITIONS[index][0] + INPUT_SHIFT_X;
                int y = yOffset + SCTCraftingMenu.INPUT_SLOT_POSITIONS[index][1];
                // JEI рисует ингредиент 16x16 по позиции слота, а рамка слота 18x18,
                // поэтому сдвигаем рамку на пиксель влево/вверх — предмет встаёт по центру.
                slotFrame.draw(graphics, x - 1, y - 1);
            }
            outputFrame.draw(graphics, xOffset + OUTPUT_SLOT_X - 5, yOffset + OUTPUT_SLOT_Y - 5);
        }
    }
}
