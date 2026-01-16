package com.ptdteam.magicadventurecore.integration.jei;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.registry.MACItems;
import com.ptdteam.magicadventurecore.world.menu.SCTCraftingMenu;
import com.ptdteam.magicadventurecore.world.recipe.SCTHybridRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public class SCTJeiCategory implements IRecipeCategory<SCTHybridRecipe> {
    public static final RecipeType<SCTHybridRecipe> TYPE =
            RecipeType.create(MagicAdventureCore.MOD_ID, "sct_hybrid_crafting", SCTHybridRecipe.class);

    private static final int GRID_START_X = 8;
    private static final int GRID_START_Y = 4;
    private static final int SLOT_SIZE = 18;
    private static final int BLOOD_SLOT_X = 116;
    private static final int BLOOD_SLOT_Y = 12;
    private static final int MANA_SLOT_X = 116;
    private static final int MANA_SLOT_Y = 48;
    private static final int OUTPUT_SLOT_X = 164;
    private static final int OUTPUT_SLOT_Y = 30;

    private final IDrawable background;
    private final IDrawable icon;

    public SCTJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(200, 110);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(MACItems.SCT.get()));
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
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SCTHybridRecipe recipe, IFocusGroup focuses) {
        List<Ingredient> ingredients = recipe.getIngredients();
        for (int row = 0; row < SCTCraftingMenu.GRID_ROWS; row++) {
            for (int col = 0; col < SCTCraftingMenu.GRID_COLUMNS; col++) {
                int index = row * SCTCraftingMenu.GRID_COLUMNS + col;
                Ingredient ingredient = ingredients.size() > index ? ingredients.get(index) : Ingredient.EMPTY;
                builder.addSlot(RecipeIngredientRole.INPUT, GRID_START_X + col * SLOT_SIZE, GRID_START_Y + row * SLOT_SIZE)
                        .addIngredients(ingredient);
            }
        }
        if (ingredients.size() > SCTCraftingMenu.BLOOD_SLOT) {
            builder.addSlot(RecipeIngredientRole.INPUT, BLOOD_SLOT_X, BLOOD_SLOT_Y)
                    .addIngredients(ingredients.get(SCTCraftingMenu.BLOOD_SLOT));
        }
        if (ingredients.size() > SCTCraftingMenu.MANA_SLOT) {
            builder.addSlot(RecipeIngredientRole.INPUT, MANA_SLOT_X, MANA_SLOT_Y)
                    .addIngredients(ingredients.get(SCTCraftingMenu.MANA_SLOT));
        }

        ItemStack result = recipe.getResultItem(RegistryAccess.EMPTY);
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y)
                .addItemStack(result);
    }
}