package com.ptdteam.magicadventurecore.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ptdteam.magicadventurecore.registry.MACRecipeTypes;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class SCTRecipeBuilder implements FinishedRecipe {
    private final ResourceLocation id;
    private final List<Ingredient> ingredients;
    private final Optional<Ingredient> bloodOrb;
    private final Optional<Ingredient> manaMirror;
    private final ItemStack result;
    private final int bloodCost;
    private final int manaCost;

    public SCTRecipeBuilder(
            ResourceLocation id,
            List<Ingredient> ingredients,
            Optional<Ingredient> bloodOrb,
            Optional<Ingredient> manaMirror,
            ItemStack result,
            int bloodCost,
            int manaCost
    ) {
        this.id = id;
        this.ingredients = ingredients;
        this.bloodOrb = bloodOrb;
        this.manaMirror = manaMirror;
        this.result = result;
        this.bloodCost = bloodCost;
        this.manaCost = manaCost;
    }

    @Override
    public void serializeRecipeData(JsonObject json) {
        JsonArray ingredientsJson = new JsonArray();
        for (Ingredient ingredient : ingredients) {
            ingredientsJson.add(ingredient.toJson());
        }
        json.add("ingredients", ingredientsJson);
        bloodOrb.ifPresent(ingredient -> json.add("blood_orb", ingredient.toJson()));
        manaMirror.ifPresent(ingredient -> json.add("mana_mirror", ingredient.toJson()));
        if (bloodCost > 0) {
            json.addProperty("blood_cost", bloodCost);
        }
        if (manaCost > 0) {
            json.addProperty("mana_cost", manaCost);
        }

        JsonObject resultJson = new JsonObject();
        resultJson.addProperty("item", result.getItem().builtInRegistryHolder().key().location().toString());
        if (result.getCount() > 1) {
            resultJson.addProperty("count", result.getCount());
        }
        json.add("result", resultJson);
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getType() {
        return MACRecipeTypes.MAC_HYBRID_SERIALIZER.get();
    }

    @Nullable
    @Override
    public JsonObject serializeAdvancement() {
        return null;
    }

    @Nullable
    @Override
    public ResourceLocation getAdvancementId() {
        return null;
    }
}