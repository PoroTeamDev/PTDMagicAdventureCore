package com.ptdteam.magicadventurecore.world.menu;

import com.ptdteam.magicadventurecore.registry.MACMenus;
import com.ptdteam.magicadventurecore.registry.MACRecipeTypes;
import com.ptdteam.magicadventurecore.world.block.entity.SCTBlockEntity;
import com.ptdteam.magicadventurecore.world.recipe.SCTHybridRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import vazkii.botania.api.mana.ManaItemHandler;
import wayoftime.bloodmagic.common.item.IBloodOrb;
import wayoftime.bloodmagic.core.data.Binding;
import wayoftime.bloodmagic.core.data.SoulNetwork;
import wayoftime.bloodmagic.util.helper.NetworkHelper;

import java.util.List;
import java.util.Optional;

public class SCTCraftingMenu extends AbstractContainerMenu {
    public static final int GRID_ROWS = 5;
    public static final int GRID_COLUMNS = 5;
    public static final int INPUT_SLOTS = GRID_ROWS * GRID_COLUMNS;
    public static final int BLOOD_SLOT = 25;
    public static final int MANA_SLOT = 26;
    public static final int OUTPUT_SLOT = 27;

    private final SCTBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final RecipeManager recipeManager;
    private final Player player;
    private Optional<SCTHybridRecipe> lastRecipe = Optional.empty();

    public SCTCraftingMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, getBlockEntity(inventory, buffer));
    }

    public SCTCraftingMenu(int containerId, Inventory inventory, SCTBlockEntity blockEntity) {
        super(MACMenus.SCT_CRAFTING_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.player = inventory.player;
        this.data = new SCTCraftingData(blockEntity.getInventory(), player);
        this.recipeManager = blockEntity.getLevel().getRecipeManager();

        addDataSlots(data);
        addCraftingSlots(blockEntity.getInventory());
        addPlayerInventory(inventory);
    }


    private static SCTBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        Level level = inventory.player.level();
        if (level.getBlockEntity(buffer.readBlockPos()) instanceof SCTBlockEntity entity) {
            return entity;
        }
        throw new IllegalStateException("SCT block entity not found");
    }

    private void addCraftingSlots(ItemStackHandler handler) {
        int startX = 8;
        int startY = 18;
        int slotSize = 18;

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                int index = row * GRID_COLUMNS + col;
                int x = startX + col * slotSize;
                int y = startY + row * slotSize;
                addSlot(new SlotItemHandler(handler, index, x, y));
            }
        }

        addSlot(new SlotItemHandler(handler, BLOOD_SLOT, 116, 26));
        addSlot(new SlotItemHandler(handler, MANA_SLOT, 116, 62));
        addSlot(new SCTCraftingResultSlot(handler, OUTPUT_SLOT, 164, 44));
    }

    private void addPlayerInventory(Inventory inventory) {
        int startX = 8;
        int startY = 142;
        int slotSize = 18;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, startX + col * slotSize, startY + row * slotSize));
            }
        }

        int hotbarY = 200;
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, startX + col * slotSize, hotbarY));
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (blockEntity.getLevel() == null) {
            return;
        }
        updateRecipeOutput();
    }

    private void updateRecipeOutput() {
        ItemStackHandler handler = blockEntity.getInventory();
        Container inputContainer = new SCTCraftingContainer(handler);
        Optional<SCTHybridRecipe> recipe = recipeManager.getRecipeFor(MACRecipeTypes.MAC_HYBRID_TYPE.get(), inputContainer, blockEntity.getLevel());
        lastRecipe = recipe;
        ItemStack result = ItemStack.EMPTY;
        if (recipe.isPresent() && hasEnoughResources(recipe.get())) {
            result = recipe.get().assemble(inputContainer, blockEntity.getLevel().registryAccess());
        }
        handler.setStackInSlot(OUTPUT_SLOT, result);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, blockEntity.getBlockState().getBlock());
    }

    public int getBloodFill() {
        return data.get(0);
    }

    public int getManaFill() {
        return data.get(1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return stack;
        }
        ItemStack slotStack = slot.getItem();
        stack = slotStack.copy();

        int containerSlots = INPUT_SLOTS + 2 + 1;
        if (index < containerSlots) {
            if (!moveItemStackTo(slotStack, containerSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!moveItemStackTo(slotStack, 0, INPUT_SLOTS, false)
                    && !moveItemStackTo(slotStack, INPUT_SLOTS, INPUT_SLOTS + 2, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return stack;
    }

    private class SCTCraftingResultSlot extends SlotItemHandler {
        public SCTCraftingResultSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            consumeIngredients();
        }
    }

    private void consumeIngredients() {
        ItemStackHandler handler = blockEntity.getInventory();
        lastRecipe.ifPresent(recipe -> {
            if (!hasEnoughResources(recipe)) {
                return;
            }
            List<net.minecraft.world.item.crafting.Ingredient> ingredients = recipe.getIngredients();
            for (int slot = 0; slot < INPUT_SLOTS + 2; slot++) {
                if (slot >= ingredients.size()) {
                    continue;
                }
                if (!ingredients.get(slot).isEmpty()) {
                    ItemStack inSlot = handler.getStackInSlot(slot);
                    if (!inSlot.isEmpty()) {
                        inSlot.shrink(1);
                        handler.setStackInSlot(slot, inSlot);
                    }
                }
            }
            consumeBlood(recipe.getBloodCost());
            consumeMana(recipe.getManaCost());
        });
        updateRecipeOutput();
    }

    private static class SCTCraftingData implements ContainerData {
        private final ItemStackHandler handler;
        private final Player player;

        private SCTCraftingData(ItemStackHandler handler, Player player) {
            this.handler = handler;
            this.player = player;
        }

        @Override
        public int get(int index) {
            if (index == 0) {
                return SCTCraftingMenu.getBloodFillPercentage(handler.getStackInSlot(BLOOD_SLOT));
            }
            if (index == 1) {
                return SCTCraftingMenu.getManaFillPercentage(handler.getStackInSlot(MANA_SLOT), player);
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    private boolean hasEnoughResources(SCTHybridRecipe recipe) {
        return hasEnoughBlood(recipe.getBloodCost()) && hasEnoughMana(recipe.getManaCost());
    }

    private boolean hasEnoughBlood(int cost) {
        if (cost <= 0) {
            return true;
        }
        ItemStack stack = blockEntity.getInventory().getStackInSlot(BLOOD_SLOT);
        if (stack.isEmpty() || !(stack.getItem() instanceof IBloodOrb)) {
            return false;
        }
        Binding binding = Binding.fromStack(stack);
        if (binding == null || binding.getOwnerId() == null) {
            return false;
        }
        SoulNetwork network = NetworkHelper.getSoulNetwork(binding);
        if (network == null) {
            return false;
        }
        return network.getCurrentEssence() >= cost;
    }

    private boolean hasEnoughMana(int cost) {
        if (cost <= 0) {
            return true;
        }
        ItemStack stack = blockEntity.getInventory().getStackInSlot(MANA_SLOT);
        if (stack.isEmpty()) {
            return false;
        }
        return ManaItemHandler.instance().requestManaExact(stack, player, cost, false);
    }

    private void consumeBlood(int cost) {
        if (cost <= 0) {
            return;
        }
        ItemStack stack = blockEntity.getInventory().getStackInSlot(BLOOD_SLOT);
        if (stack.isEmpty() || !(stack.getItem() instanceof IBloodOrb)) {
            return;
        }
        Binding binding = Binding.fromStack(stack);
        if (binding == null || binding.getOwnerId() == null) {
            return;
        }
        SoulNetwork network = NetworkHelper.getSoulNetwork(binding);
        if (network != null) {
            network.syphon(cost);
        }
    }

    private void consumeMana(int cost) {
        if (cost <= 0) {
            return;
        }
        ItemStack stack = blockEntity.getInventory().getStackInSlot(MANA_SLOT);
        if (!stack.isEmpty()) {
            ManaItemHandler.instance().requestManaExact(stack, player, cost, true);
        }

    }

    private static int getBloodFillPercentage(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IBloodOrb)) {
            return 0;
        }
        Binding binding = Binding.fromStack(stack);
        if (binding == null || binding.getOwnerId() == null) {
            return 0;
        }
        SoulNetwork network = NetworkHelper.getSoulNetwork(binding);
        if (network == null) {
            return 0;
        }
        int max = NetworkHelper.getMaximumForTier(network.getOrbTier());
        int current = network.getCurrentEssence();
        return max > 0 ? Math.min(100, Math.max(0, current * 100 / max)) : 0;
    }

    private static int getManaFillPercentage(ItemStack stack, Player player) {
        if (stack.isEmpty()) {
            return 0;
        }
        if (stack.hasTag()) {
            var tag = stack.getTag();
            if (tag != null) {
                if (tag.contains("mana") && tag.contains("maxMana")) {
                    int max = tag.getInt("maxMana");
                    int current = tag.getInt("mana");
                    return max > 0 ? Math.min(100, Math.max(0, current * 100 / max)) : 0;
                }
                if (tag.contains("Mana") && tag.contains("MaxMana")) {
                    int max = tag.getInt("MaxMana");
                    int current = tag.getInt("Mana");
                    return max > 0 ? Math.min(100, Math.max(0, current * 100 / max)) : 0;
                }
                if (tag.contains("LP") && tag.contains("MaxLP")) {
                    int max = tag.getInt("MaxLP");
                    int current = tag.getInt("LP");
                    return max > 0 ? Math.min(100, Math.max(0, current * 100 / max)) : 0;
                }
                if (tag.contains("lifeEssence") && tag.contains("capacity")) {
                    int max = tag.getInt("capacity");
                    int current = tag.getInt("lifeEssence");
                    return max > 0 ? Math.min(100, Math.max(0, current * 100 / max)) : 0;
                }
            }
        }
        if (player != null) {
            int estimateMax = 10000;
            int possible = ManaItemHandler.instance().requestMana(stack, player, estimateMax, false);
            return possible > 0 ? Math.min(100, Math.max(0, possible * 100 / estimateMax)) : 0;
        }
        return 100;

    }
}