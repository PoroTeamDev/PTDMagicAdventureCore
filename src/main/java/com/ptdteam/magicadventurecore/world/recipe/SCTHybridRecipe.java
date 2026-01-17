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

import java.util.List;

public class SCTHybridRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final Ingredient bloodOrbIngredient;
    private final Ingredient manaMirrorIngredient;
    private final int bloodCost;
    private final int manaCost;

    public SCTHybridRecipe(
            ResourceLocation id,
            NonNullList<Ingredient> ingredients,
            Ingredient bloodOrbIngredient,
            Ingredient manaMirrorIngredient,
            ItemStack result,
            int bloodCost,
            int manaCost
    ) {
        this.id = id;
        this.ingredients = ingredients;
        this.result = result;
        this.bloodOrbIngredient = bloodOrbIngredient;
        this.manaMirrorIngredient = manaMirrorIngredient;
        this.bloodCost = bloodCost;
        this.manaCost = manaCost;
    }

    @Override
    public boolean matches(Container container, Level level) {
        List<Ingredient> required = ingredients.stream()
                .filter(ingredient -> !ingredient.isEmpty())
                .toList();
        List<ItemStack> inputs = new java.util.ArrayList<>();
        for (int slot = 0; slot < SCTCraftingMenu.INPUT_SLOTS; slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                inputs.add(stack);
            }
        }
        if (inputs.size() != required.size()) {
            return false;
        }
        List<ItemStack> remaining = new java.util.ArrayList<>(inputs);
        for (Ingredient ingredient : required) {
            boolean matched = false;
            for (int i = 0; i < remaining.size(); i++) {
                if (ingredient.test(remaining.get(i))) {
                    remaining.remove(i);
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }
        return remaining.isEmpty();
    }

    @Override
    public ItemStack assemble(Container container, net.minecraft.core.RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= SCTCraftingMenu.INPUT_SLOTS;
    }

    @Override
    public boolean isSpecial() {
        return true;
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

    public Ingredient getBloodOrbIngredient() {
        return bloodOrbIngredient;
    }

    public Ingredient getManaMirrorIngredient() {
        return manaMirrorIngredient;
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