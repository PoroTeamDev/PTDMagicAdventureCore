package com.ptdteam.magicadventurecore.integration.jei;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.registry.MACItems;
import com.ptdteam.magicadventurecore.registry.MACRecipeTypes;
import com.ptdteam.magicadventurecore.world.recipe.SCTHybridRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

@JeiPlugin
public class SCTJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(MagicAdventureCore.MOD_ID, "sct");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new SCTJeiCategory(registration.getJeiHelpers().getGuiHelper()),
                new SCTRitualCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Отдельная категория-схема ритуала (одна запись).
        registration.addRecipes(SCTRitualCategory.TYPE,
                List.of(SCTRitualCategory.RitualInfo.INSTANCE));

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }
        RecipeManager recipeManager = connection.getRecipeManager();
        List<SCTHybridRecipe> recipes = recipeManager.getAllRecipesFor(MACRecipeTypes.MAC_HYBRID_TYPE.get());
        registration.addRecipes(SCTJeiCategory.TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // По этим предметам JEI покажет категорию ритуала (R / U).
        registration.addRecipeCatalyst(new ItemStack(MACItems.SCT.get()), SCTRitualCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(MACItems.ARCANE_CHALK.get()), SCTRitualCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(MACItems.MAGE_STABILIZER.get()), SCTRitualCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(MACItems.SCT_FRAME.get()), SCTRitualCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(MACItems.SCT_MANA_RESERVOIR.get()), SCTRitualCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(MACItems.SCT_BLOOD_RESERVOIR.get()), SCTRitualCategory.TYPE);
        registration.addRecipeCatalyst(new ItemStack(MACItems.SCT_ACCELERATOR.get()), SCTRitualCategory.TYPE);
    }
}
