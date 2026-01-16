package com.ptdteam.magicadventurecore.world.recipe;

import com.ptdteam.magicadventurecore.registry.*;
import com.ptdteam.magicadventurecore.world.menu.SCTCraftingMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class SCTHybridRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final int bloodCost;
    private final int manaCost;

    public SCTHybridRecipe(
            ResourceLocation id,
            NonNullList<Ingredient> ingredients,
            ItemStack result,
            int bloodCost,
            int manaCost
    ) {
        this.id = id;
        this.ingredients = ingredients;
        this.result = result;
        this.bloodCost = bloodCost;
        this.manaCost = manaCost;
    }

    @Override
    public boolean matches(Container container, Level level) {
        for (int slot = 0; slot < SCTCraftingMenu.INPUT_SLOTS + 2; slot++) {
            if (slot >= ingredients.size()) {
                return false;
            }
            Ingredient ingredient = ingredients.get(slot);
            if (!ingredient.isEmpty() && !ingredient.test(container.getItem(slot))) {
                return false;
            }
            if (ingredient.isEmpty() && !container.getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(Container container, net.minecraft.core.RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 5 && height >= 5;
    }

    @Override
    public ItemStack getResultItem(net.minecraft.core.RegistryAccess registryAccess) {
        return result;
    }

    public int getBloodCost() {
        return bloodCost;
    }

    public int getManaCost() {
        return manaCost;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return MACRecipeTypes.MAC_HYBRID_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return MACRecipeTypes.MAC_HYBRID_TYPE.get();
    }
}