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
    /** Длительность «крафтовой» анимации по умолчанию, в тиках (6 секунд). */
    public static final int DEFAULT_CRAFT_TIME = 120;
    /** Верхняя граница, чтобы датапак не повесил GUI на минуту. */
    public static final int MAX_CRAFT_TIME = 1200;

    private final ResourceLocation id;
    private final NonNullList<Ingredient> ingredients;
    private final ItemStack result;
    private final Ingredient bloodOrbIngredient;
    private final Ingredient manaMirrorIngredient;
    private final int bloodCost;
    private final int manaCost;
    private final int craftTime;
    /**
     * Стоимость стабилизации. 0 — простой рецепт, работает на голом столе.
     * Больше 0 — стабилизированный: нужна платформа 3×3, валидный круг мела
     * и 4 заряженных стабилизатора.
     */
    private final int stability;

    public SCTHybridRecipe(
            ResourceLocation id,
            NonNullList<Ingredient> ingredients,
            Ingredient bloodOrbIngredient,
            Ingredient manaMirrorIngredient,
            ItemStack result,
            int bloodCost,
            int manaCost,
            int craftTime,
            int stability
    ) {
        this.id = id;
        this.ingredients = ingredients;
        this.result = result;
        this.bloodOrbIngredient = bloodOrbIngredient;
        this.manaMirrorIngredient = manaMirrorIngredient;
        this.bloodCost = bloodCost;
        this.manaCost = manaCost;
        this.craftTime = craftTime;
        this.stability = stability;
    }

    /** Сколько тиков играет анимация крафта. 0 — крафт мгновенный. */
    public int getCraftTime() {
        return craftTime;
    }

    /** Стоимость стабилизации (0 — простой рецепт, без ритуала). */
    public int getStability() {
        return stability;
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
        if (!remaining.isEmpty()) {
            return false;
        }
        // Слоты сферы и мана-предмета намеренно не проверяются здесь: их наличие и стоимость
        // проверяет SCTCraftingMenu#hasEnoughResources. Жёсткая сверка с конкретным предметом
        // из JSON не давала использовать сферу другого уровня.
        return true;
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
        return result.copy();
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