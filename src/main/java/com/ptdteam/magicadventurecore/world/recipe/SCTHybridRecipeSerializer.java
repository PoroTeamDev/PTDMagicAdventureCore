package com.ptdteam.magicadventurecore.world.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.ptdteam.magicadventurecore.world.menu.SCTCraftingMenu;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

public class SCTHybridRecipeSerializer implements RecipeSerializer<SCTHybridRecipe> {
    @Override
    public SCTHybridRecipe fromJson(ResourceLocation id, JsonObject json) {
        JsonArray ingredientsJson = GsonHelper.getAsJsonArray(json, "ingredients");
        if (ingredientsJson.size() != SCTCraftingMenu.INPUT_SLOTS) {
            throw new IllegalArgumentException("SCT hybrid recipe requires exactly 14 ingredients");
        }
        int bloodCost = GsonHelper.getAsInt(json, "blood_cost", 0);
        int manaCost = GsonHelper.getAsInt(json, "mana_cost", 0);
        NonNullList<Ingredient> ingredients = NonNullList.withSize(SCTCraftingMenu.INPUT_SLOTS, Ingredient.EMPTY);
        for (int i = 0; i < ingredientsJson.size(); i++) {
            ingredients.set(i, Ingredient.fromJson(ingredientsJson.get(i)));
        }
        Ingredient bloodOrb = json.has("blood_orb") ? Ingredient.fromJson(json.get("blood_orb")) : Ingredient.EMPTY;
        Ingredient manaMirror = json.has("mana_mirror") ? Ingredient.fromJson(json.get("mana_mirror")) : Ingredient.EMPTY;
        ItemStack result = ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
        return new SCTHybridRecipe(id, ingredients, bloodOrb, manaMirror, result, bloodCost, manaCost);
    }

    @Nullable
    @Override
    public SCTHybridRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        int bloodCost = buffer.readVarInt();
        int manaCost = buffer.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.withSize(size, Ingredient.EMPTY);
        for (int i = 0; i < size; i++) {
            ingredients.set(i, Ingredient.fromNetwork(buffer));
        }
        Ingredient bloodOrb = Ingredient.fromNetwork(buffer);
        Ingredient manaMirror = Ingredient.fromNetwork(buffer);
        ItemStack result = buffer.readItem();
        return new SCTHybridRecipe(id, ingredients, bloodOrb, manaMirror, result, bloodCost, manaCost);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, SCTHybridRecipe recipe) {
        buffer.writeVarInt(recipe.getIngredients().size());
        buffer.writeVarInt(recipe.getBloodCost());
        buffer.writeVarInt(recipe.getManaCost());
        for (Ingredient ingredient : recipe.getIngredients()) {
            ingredient.toNetwork(buffer);
        }
        recipe.getBloodOrbIngredient().toNetwork(buffer);
        recipe.getManaMirrorIngredient().toNetwork(buffer);
        buffer.writeItem(recipe.getResultItem(net.minecraft.core.RegistryAccess.EMPTY));
    }
}