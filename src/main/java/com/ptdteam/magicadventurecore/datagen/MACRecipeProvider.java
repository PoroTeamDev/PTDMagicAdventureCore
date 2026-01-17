package com.ptdteam.magicadventurecore.datagen;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class MACRecipeProvider extends RecipeProvider {
    public MACRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        Item bloodOrbItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("bloodmagic", "weakbloodorb"));
        Item manaMirrorItem = ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath("botania", "mana_mirror"));
        Ingredient bloodOrb = bloodOrbItem == null ? Ingredient.EMPTY : Ingredient.of(bloodOrbItem);
        Ingredient manaMirror = manaMirrorItem == null ? Ingredient.EMPTY : Ingredient.of(manaMirrorItem);
        List<Ingredient> ingredients = List.of(
                Ingredient.of(Items.STONE), Ingredient.of(Items.STONE), Ingredient.of(Items.STONE), Ingredient.of(Items.STONE),
                Ingredient.of(Items.STONE), Ingredient.of(Items.STONE), Ingredient.of(Items.STONE), Ingredient.of(Items.STONE),
                Ingredient.of(Items.STONE), Ingredient.of(Items.STONE), Ingredient.of(Items.STONE), Ingredient.of(Items.STONE),
                Ingredient.of(Items.STONE), Ingredient.of(Items.STONE)
        );
        SCTRecipeBuilder recipe = new SCTRecipeBuilder(
                ResourceLocation.fromNamespaceAndPath(MagicAdventureCore.MOD_ID, "sct_test"),
                ingredients,
                Optional.of(bloodOrb),
                Optional.of(manaMirror),
                new ItemStack(Items.DIAMOND),
                5000,
                25000
        );
        consumer.accept(recipe);
    }
}