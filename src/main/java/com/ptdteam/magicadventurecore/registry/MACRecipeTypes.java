package com.ptdteam.magicadventurecore.registry;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.world.recipe.SCTHybridRecipe;
import com.ptdteam.magicadventurecore.world.recipe.SCTHybridRecipeSerializer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class MACRecipeTypes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MagicAdventureCore.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MagicAdventureCore.MOD_ID);

    public static final RegistryObject<RecipeSerializer<SCTHybridRecipe>> MAC_HYBRID_SERIALIZER =
            SERIALIZERS.register("sct_hybrid_crafting", SCTHybridRecipeSerializer::new);
    public static final RegistryObject<RecipeType<SCTHybridRecipe>> MAC_HYBRID_TYPE =
            TYPES.register("sct_hybrid_crafting", () -> new RecipeType<>() {});

    private MACRecipeTypes() {
    }

    public static void register(net.minecraftforge.eventbus.api.IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}