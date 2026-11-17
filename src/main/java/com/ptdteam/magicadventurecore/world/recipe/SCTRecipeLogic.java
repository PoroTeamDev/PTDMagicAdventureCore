package com.ptdteam.magicadventurecore.world.recipe;

import com.ptdteam.magicadventurecore.registry.MACRecipeTypes;
import com.ptdteam.magicadventurecore.world.block.entity.SCTBlockEntity;
import com.ptdteam.magicadventurecore.world.menu.SCTCraftingContainer;
import com.ptdteam.magicadventurecore.world.menu.SCTCraftingMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaItem;
import vazkii.botania.api.mana.ManaItemHandler;
import vazkii.botania.api.mana.ManaPool;
import wayoftime.bloodmagic.common.item.IBloodOrb;
import wayoftime.bloodmagic.core.data.Binding;
import wayoftime.bloodmagic.core.data.SoulNetwork;
import wayoftime.bloodmagic.util.helper.NetworkHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Общая логика крафта SCT: поиск рецепта, проверка и списание крови/маны/ингредиентов,
 * пересчёт результата. Используется и меню (GUI), и блок-сущностью (завершение анимации
 * уже после закрытия GUI), поэтому не завязана на конкретный экземпляр меню.
 */
public final class SCTRecipeLogic {
    private SCTRecipeLogic() {
    }

    public static Optional<SCTHybridRecipe> findRecipe(Level level, ItemStackHandler handler) {
        if (level == null) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(
                MACRecipeTypes.MAC_HYBRID_TYPE.get(),
                new SCTCraftingContainer(handler),
                level
        );
    }

    /** Проверка только мана-предмета/пула (буфер учитывает блок-сущность). */
    public static boolean hasEnoughManaItem(ItemStack manaStack, @Nullable Player player, @Nullable Level level, int cost) {
        if (cost <= 0) {
            return true;
        }
        if (manaStack.isEmpty()) {
            return false;
        }
        ManaPool pool = resolveBoundManaPool(manaStack, level);
        if (pool != null) {
            return pool.getCurrentMana() >= cost;
        }
        if (getBotaniaTagMana(manaStack) >= cost) {
            return true;
        }
        ManaItem manaItem = manaStack.getCapability(BotaniaForgeCapabilities.MANA_ITEM).resolve().orElse(null);
        if (manaItem != null && manaItem.getMana() >= cost) {
            return true;
        }
        return player != null && ManaItemHandler.instance().requestManaExact(manaStack, player, cost, false);
    }

    public static void consumeManaItem(ItemStack manaStack, @Nullable Player player, @Nullable Level level, int cost) {
        if (cost <= 0 || manaStack.isEmpty()) {
            return;
        }
        // Оплата идёт из одного источника: сначала привязанный мана-пул, иначе из самого предмета.
        ManaPool pool = resolveBoundManaPool(manaStack, level);
        if (pool != null && pool.getCurrentMana() >= cost) {
            pool.receiveMana(-cost);
            return;
        }
        int stored = getBotaniaTagMana(manaStack);
        if (stored >= cost) {
            manaStack.getTag().putInt("mana", stored - cost);
            return;
        }
        ManaItem manaItem = manaStack.getCapability(BotaniaForgeCapabilities.MANA_ITEM).resolve().orElse(null);
        if (manaItem != null && manaItem.getMana() >= cost) {
            manaItem.addMana(-cost);
            return;
        }
        if (player != null) {
            ManaItemHandler.instance().requestManaExact(manaStack, player, cost, true);
        }
    }

    public static void consumeIngredients(SCTHybridRecipe recipe, ItemStackHandler handler) {
        List<Ingredient> remaining = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (!ingredient.isEmpty()) {
                remaining.add(ingredient);
            }
        }
        for (int slot = 0; slot < SCTCraftingMenu.INPUT_SLOTS; slot++) {
            ItemStack inSlot = handler.getStackInSlot(slot);
            if (inSlot.isEmpty()) {
                continue;
            }
            for (int i = 0; i < remaining.size(); i++) {
                if (remaining.get(i).test(inSlot)) {
                    ItemStack updated = inSlot.copy();
                    updated.shrink(1);
                    handler.setStackInSlot(slot, updated);
                    remaining.remove(i);
                    break;
                }
            }
        }
    }

    /**
     * Пересчитывает слот результата из текущей сетки (сервер). После списания ингредиентов
     * старый результат обязан исчезнуть, иначе его можно было бы забрать бесплатно.
     */
    public static void refreshOutput(Level level, ItemStackHandler handler) {
        if (level == null || level.isClientSide) {
            return;
        }
        Optional<SCTHybridRecipe> recipe = findRecipe(level, handler);
        ItemStack result = recipe
                .map(found -> found.assemble(new SCTCraftingContainer(handler), level.registryAccess()))
                .orElse(ItemStack.EMPTY);
        ItemStack current = handler.getStackInSlot(SCTBlockEntity.OUTPUT_SLOT);
        if (!ItemStack.matches(current, result)) {
            handler.setStackInSlot(SCTBlockEntity.OUTPUT_SLOT, result);
        }
    }

    /** Выдаёт результат крафта игроку, иначе роняет его у блока. */
    public static void deliver(ItemStack result, @Nullable Player player, Level level, BlockPos pos) {
        if (result.isEmpty()) {
            return;
        }
        if (player != null && !player.level().isClientSide) {
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
            return;
        }
        Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), result);
    }

    // ------------------------------------------------------------------ кровь

    public static boolean hasEnoughBlood(ItemStack orb, int cost) {
        if (cost <= 0) {
            return true;
        }
        SoulNetwork network = getSoulNetwork(orb);
        return network != null && network.getCurrentEssence() >= cost;
    }

    public static void consumeBlood(ItemStack orb, int cost) {
        if (cost <= 0) {
            return;
        }
        SoulNetwork network = getSoulNetwork(orb);
        if (network != null) {
            network.syphon(cost);
        }
    }

    @Nullable
    public static SoulNetwork getSoulNetwork(ItemStack orb) {
        if (orb.isEmpty() || !(orb.getItem() instanceof IBloodOrb)) {
            return null;
        }
        Binding binding = Binding.fromStack(orb);
        if (binding == null || binding.getOwnerId() == null) {
            return null;
        }
        return NetworkHelper.getSoulNetwork(binding);
    }

    public static int getBloodAmount(ItemStack orb) {
        SoulNetwork network = getSoulNetwork(orb);
        return network == null ? 0 : network.getCurrentEssence();
    }

    public static int getBloodCapacity(ItemStack orb) {
        SoulNetwork network = getSoulNetwork(orb);
        return network == null ? 0 : NetworkHelper.getMaximumForTier(network.getOrbTier());
    }

    public static int getBloodFillPercentage(ItemStack orb) {
        int max = getBloodCapacity(orb);
        return max > 0 ? Math.min(100, Math.max(0, getBloodAmount(orb) * 100 / max)) : 0;
    }

    public static boolean isBloodItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof IBloodOrb;
    }

    // ------------------------------------------------------------------- мана

    /** Мана, закэшированная в NBT самого botania-предмета; для чужих предметов всегда 0. */
    public static int getBotaniaTagMana(ItemStack stack) {
        if (!isBotaniaItem(stack)) {
            return 0;
        }
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt("mana");
    }

    public static boolean isBotaniaItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null && "botania".equals(id.getNamespace());
    }

    public static int getManaAmount(ItemStack mirror, @Nullable Level level) {
        ManaPool pool = resolveBoundManaPool(mirror, level);
        if (pool != null) {
            return pool.getCurrentMana();
        }
        CompoundTag tag = mirror.getTag();
        return tag == null ? 0 : tag.getInt("mana");
    }

    public static int getManaCapacity(ItemStack mirror, @Nullable Level level) {
        ManaPool pool = resolveBoundManaPool(mirror, level);
        if (pool != null) {
            return pool.getMaxMana();
        }
        CompoundTag tag = mirror.getTag();
        return tag == null ? 0 : tag.getInt("maxMana");
    }

    public static int getManaFillPercentage(ItemStack mirror, @Nullable Player player, @Nullable Level level) {
        if (mirror.isEmpty()) {
            return 0;
        }
        int capacity = getManaCapacity(mirror, level);
        int amount = getManaAmount(mirror, level);
        if (capacity > 0) {
            return Math.min(100, Math.max(0, amount * 100 / capacity));
        }
        if (player != null) {
            int estimateMax = 10000;
            int possible = ManaItemHandler.instance().requestMana(mirror, player, estimateMax, false);
            return possible > 0 ? Math.min(100, Math.max(0, possible * 100 / estimateMax)) : 0;
        }
        return 0;
    }

    public static boolean isManaItem(ItemStack stack, @Nullable Player player) {
        if (stack.isEmpty()) {
            return false;
        }
        // Любой предмет с Botania-капабилити MANA_ITEM: кольца, планшет, зеркало и т.п.
        if (stack.getCapability(BotaniaForgeCapabilities.MANA_ITEM).isPresent()) {
            return true;
        }
        if (player != null && ManaItemHandler.instance().requestManaExact(stack, player, 0, false)) {
            return true;
        }
        // Мана-зеркало с привязкой к пулу в "pos" (если капабилити не выставлена).
        return isBotaniaItem(stack) && stack.hasTag() && stack.getTag().contains("pos");
    }

    /**
     * Разрешает мана-пул, к которому привязано зеркало. Botania хранит привязку как
     * {@code GlobalPos}: {@code pos:{dimension:"<id>", pos:[x,y,z]}}.
     */
    @Nullable
    public static ManaPool resolveBoundManaPool(ItemStack mirror, @Nullable Level level) {
        if (level == null || mirror.isEmpty()) {
            return null;
        }
        CompoundTag tag = mirror.getTag();
        if (tag == null || !tag.contains("pos", Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag posTag = tag.getCompound("pos");
        if (!posTag.contains("dimension", Tag.TAG_STRING) || !posTag.contains("pos", Tag.TAG_INT_ARRAY)) {
            return null;
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(posTag.getString("dimension"));
        int[] coords = posTag.getIntArray("pos");
        if (dimensionId == null || coords.length < 3) {
            return null;
        }
        Level poolLevel = level;
        if (!level.dimension().location().equals(dimensionId)) {
            MinecraftServer server = level.getServer();
            if (server == null) {
                return null;
            }
            poolLevel = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
        }
        if (poolLevel == null) {
            return null;
        }
        BlockEntity blockEntity = poolLevel.getBlockEntity(new BlockPos(coords[0], coords[1], coords[2]));
        return blockEntity instanceof ManaPool pool ? pool : null;
    }
}
