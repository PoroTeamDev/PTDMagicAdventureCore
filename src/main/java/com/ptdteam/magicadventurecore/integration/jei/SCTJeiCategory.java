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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.List;

public class SCTJeiCategory implements IRecipeCategory<SCTHybridRecipe> {
    public static final RecipeType<SCTHybridRecipe> TYPE =
            RecipeType.create(MagicAdventureCore.MOD_ID, "sct_hybrid_crafting", SCTHybridRecipe.class);

    private static final int BLOOD_SLOT_X = SCTCraftingMenu.BLOOD_SLOT_X;
    private static final int BLOOD_SLOT_Y = SCTCraftingMenu.BLOOD_SLOT_Y;
    private static final int MANA_SLOT_X = SCTCraftingMenu.MANA_SLOT_X;
    private static final int MANA_SLOT_Y = SCTCraftingMenu.MANA_SLOT_Y;
    private static final int OUTPUT_SLOT_X = SCTCraftingMenu.OUTPUT_SLOT_X;
    private static final int OUTPUT_SLOT_Y = SCTCraftingMenu.OUTPUT_SLOT_Y;

    private final IDrawable background;
    private final IDrawable icon;

    public SCTJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(190, 140);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(MACItems.SCT.get()));
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
            int x = SCTCraftingMenu.INPUT_SLOT_POSITIONS[index][0];
            int y = SCTCraftingMenu.INPUT_SLOT_POSITIONS[index][1];
            builder.addSlot(RecipeIngredientRole.INPUT, x, y)
                    .addIngredients(ingredient);
        }
        if (!recipe.getBloodOrbIngredient().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, BLOOD_SLOT_X, BLOOD_SLOT_Y)
                    .addIngredients(recipe.getBloodOrbIngredient());
        }
        if (!recipe.getManaMirrorIngredient().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, MANA_SLOT_X, MANA_SLOT_Y)
                    .addIngredients(recipe.getManaMirrorIngredient());
        }

        ItemStack result = recipe.getResultItem(RegistryAccess.EMPTY);
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y)
                .addItemStack(result);
    }
}