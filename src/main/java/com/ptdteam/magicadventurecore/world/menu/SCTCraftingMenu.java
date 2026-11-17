package com.ptdteam.magicadventurecore.world.menu;

import com.ptdteam.magicadventurecore.registry.MACMenus;
import com.ptdteam.magicadventurecore.world.block.entity.MageStabilizerBlockEntity;
import com.ptdteam.magicadventurecore.world.block.entity.SCTBlockEntity;
import com.ptdteam.magicadventurecore.world.recipe.SCTHybridRecipe;
import com.ptdteam.magicadventurecore.world.recipe.SCTRecipeLogic;
import com.ptdteam.magicadventurecore.world.ritual.ChalkCircle;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.Optional;

public class SCTCraftingMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOTS = 14;
    public static final int BLOOD_SLOT = 14;
    public static final int MANA_SLOT = 15;
    public static final int OUTPUT_SLOT = 16;
    /** Индекс первого слота инвентаря игрока в меню (все слоты машины идут раньше). */
    public static final int PLAYER_SLOT_START = OUTPUT_SLOT + 1;
    public static final int[][] INPUT_SLOT_POSITIONS = {
            {80, 22},
            {100, 29},
            {119, 40},
            {126, 59},
            {126, 78},
            {119, 97},
            {100, 108},
            {80, 114},
            {60, 108},
            {41, 97},
            {34, 78},
            {34, 59},
            {41, 40},
            {60, 29}
    };
    public static final int BLOOD_SLOT_X = 9;
    public static final int BLOOD_SLOT_Y = 68;
    public static final int MANA_SLOT_X = 151;
    public static final int MANA_SLOT_Y = 68;
    public static final int OUTPUT_SLOT_X = 80;
    public static final int OUTPUT_SLOT_Y = 68;

    private final SCTBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final Player player;
    private Optional<SCTHybridRecipe> lastRecipe = Optional.empty();
    /** Отпечаток содержимого 14 слотов сетки: позволяет не пересчитывать рецепт каждый тик. */
    private int lastInputFingerprint;

    public SCTCraftingMenu(int containerId, Inventory inventory, FriendlyByteBuf buffer) {
        this(containerId, inventory, getBlockEntity(inventory, buffer));
    }

    public SCTCraftingMenu(int containerId, Inventory inventory, SCTBlockEntity blockEntity) {
        super(MACMenus.SCT_CRAFTING_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.player = inventory.player;
        this.data = new SCTCraftingData(blockEntity, player);

        addDataSlots(data);
        addCraftingSlots(blockEntity.getInventory());
        addPlayerInventory(inventory);

        this.lastInputFingerprint = inputFingerprint();
        this.updateRecipeOutput();
    }

    private static SCTBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        Level level = inventory.player.level();
        if (level.getBlockEntity(buffer.readBlockPos()) instanceof SCTBlockEntity entity) {
            return entity;
        }
        throw new IllegalStateException("SCT block entity not found");
    }

    private void addCraftingSlots(ItemStackHandler handler) {
        for (int index = 0; index < INPUT_SLOTS; index++) {
            int x = INPUT_SLOT_POSITIONS[index][0];
            int y = INPUT_SLOT_POSITIONS[index][1];
            addSlot(new MachineSlot(handler, index, x, y));
        }

        addSlot(new MachineSlot(handler, BLOOD_SLOT, BLOOD_SLOT_X, BLOOD_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SCTRecipeLogic.isBloodItem(stack);
            }
        });
        addSlot(new MachineSlot(handler, MANA_SLOT, MANA_SLOT_X, MANA_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return SCTRecipeLogic.isManaItem(stack, player);
            }
        });
        addSlot(new SCTCraftingResultSlot(handler, OUTPUT_SLOT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y));
    }

    /**
     * Forge-овский {@link SlotItemHandler} прокидывает {@code setChanged()} в пустой
     * контейнер, поэтому изменения через {@code moveItemStackTo} не помечали блок-сущность
     * грязной и могли не сохраниться. Этот слот сообщает об изменениях напрямую.
     */
    private class MachineSlot extends SlotItemHandler {
        MachineSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public void setChanged() {
            blockEntity.setChanged();
        }

        /**
         * Во время анимации машина рисует предметы сама (тряска, полёт к центру),
         * поэтому ванильный рендер слота должен молчать. Только клиент.
         */
        @Override
        public ItemStack getItem() {
            if (isClientAnimation() && SCTAnimationState.isHidden(getSlotIndex())) {
                return ItemStack.EMPTY;
            }
            return super.getItem();
        }
    }

    private boolean isClientAnimation() {
        Level level = blockEntity.getLevel();
        return level != null && level.isClientSide && SCTAnimationState.isHidingActive();
    }

    private void addPlayerInventory(Inventory inventory) {
        // Координаты под новый фон 176x230: сетка 9x3 + хотбар, шаг 18 px.
        int startX = 8;
        int startY = 150;
        int slotSize = 18;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, 9 + row * 9 + col, startX + col * slotSize, startY + row * slotSize));
            }
        }

        int hotbarY = 208;
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
        this.lastInputFingerprint = inputFingerprint();
        updateRecipeOutput();
    }

    @Override
    public void broadcastChanges() {
        if (blockEntity.getLevel() != null) {
            // Сетка меняется только через слоты, но слушателя у ItemStackHandler нет,
            // поэтому дешёвый отпечаток заменяет пересчёт рецепта каждый тик.
            int fingerprint = inputFingerprint();
            if (fingerprint != lastInputFingerprint) {
                this.lastInputFingerprint = fingerprint;
                updateRecipeOutput();
            }
        }
        super.broadcastChanges();
    }

    /**
     * Взятие результата запускает анимацию крафта, а не мгновенную выдачу предмета.
     * На время анимации слоты машины заблокированы. Ванильный путь изъятия результата
     * не используется вообще — результат выдаёт блок-сущность в конце крафта.
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == OUTPUT_SLOT) {
            boolean pickupLike = clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE;
            if (pickupLike && player instanceof ServerPlayer serverPlayer) {
                tryStartCraft(serverPlayer);
            }
            return;
        }
        // На время анимации заблокированы только слоты машины; инвентарь игрока доступен.
        if (blockEntity.isCrafting() && slotId >= 0 && slotId < PLAYER_SLOT_START) {
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    private void tryStartCraft(ServerPlayer player) {
        if (blockEntity.isCrafting()) {
            return;
        }
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        if (!slots.get(OUTPUT_SLOT).hasItem()) {
            return;
        }
        Optional<SCTHybridRecipe> recipe = SCTRecipeLogic.findRecipe(level, blockEntity.getInventory());
        if (recipe.isEmpty()) {
            return;
        }
        if (!blockEntity.hasEnoughResources(recipe.get(), player)) {
            notifyDenied(player, recipe.get());
            return;
        }
        if (recipe.get().getStability() > 0) {
            if (!blockEntity.isStructureFormed()) {
                player.displayClientMessage(
                        Component.translatable("message.magicadventurecore.not_formed"), true);
                return;
            }
            if (!blockEntity.isCircleComplete()) {
                player.displayClientMessage(Component.translatable(
                        "message.magicadventurecore.circle_incomplete",
                        blockEntity.getCircleMarks(),
                        ChalkCircle.REQUIRED_MARKS), true);
                return;
            }
            if (blockEntity.getStabilizerCount() < 4) {
                player.displayClientMessage(Component.translatable(
                        "message.magicadventurecore.not_enough_stabilizers"), true);
                return;
            }
        }
        blockEntity.startCraft(player);
    }

    private void notifyDenied(Player player, SCTHybridRecipe recipe) {
        // Сначала буфер крови, остаток — из сферы (как в SCTBlockEntity#hasEnoughResources).
        int bloodRemaining = recipe.getBloodCost() - Math.min(
                blockEntity.getBloodBuffer(), recipe.getBloodCost());
        boolean enoughBlood = bloodRemaining <= 0 || SCTRecipeLogic.hasEnoughBlood(
                blockEntity.getInventory().getStackInSlot(BLOOD_SLOT), bloodRemaining);
        String key = enoughBlood
                ? "message.magicadventurecore.not_enough_mana"
                : "message.magicadventurecore.not_enough_blood";
        player.displayClientMessage(Component.translatable(key), true);
    }

    private int inputFingerprint() {
        ItemStackHandler handler = blockEntity.getInventory();
        int hash = 1;
        for (int slot = 0; slot < INPUT_SLOTS; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                hash = 31 * hash;
                continue;
            }
            hash = 31 * hash + stack.getItem().hashCode();
            hash = 31 * hash + stack.getCount();
            hash = 31 * hash + (stack.hasTag() ? stack.getTag().hashCode() : 0);
        }
        return hash;
    }

    private void updateRecipeOutput() {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }
        lastRecipe = SCTRecipeLogic.findRecipe(level, blockEntity.getInventory());
        // На клиенте инвентарь блок-сущности — зеркало серверных данных, писать в него нельзя.
        if (!level.isClientSide) {
            SCTRecipeLogic.refreshOutput(level, blockEntity.getInventory());
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, blockEntity.getBlockState().getBlock());
    }

    public SCTBlockEntity getBlockEntity() {
        return blockEntity;
    }

    /** Стоимость крови текущего рецепта (для анимации потока с шкал). */
    public int getRecipeBloodCost() {
        return lastRecipe.map(SCTHybridRecipe::getBloodCost).orElse(0);
    }

    /** Стоимость маны текущего рецепта (для анимации потока с шкал). */
    public int getRecipeManaCost() {
        return lastRecipe.map(SCTHybridRecipe::getManaCost).orElse(0);
    }

    public int getBloodFill() {
        return data.get(0);
    }

    public int getManaFill() {
        return data.get(1);
    }

    /**
     * Абсолютные значения для тултипа. Слоты данных передаются как short, поэтому
     * каждый объём кодируется двумя 15-битными половинами (до ~1 000 000).
     */
    public int getBloodAmount() {
        return (data.get(3) << 15) | data.get(2);
    }

    public int getBloodCapacity() {
        return (data.get(7) << 15) | data.get(6);
    }

    public int getManaAmount() {
        return (data.get(5) << 15) | data.get(4);
    }

    public int getManaCapacity() {
        return (data.get(9) << 15) | data.get(8);
    }

    /** Собрана ли платформа 3×3 (кольцо рамок или модулей). */
    public boolean isStructureFormed() {
        return data.get(10) != 0;
    }

    public int getModuleCount() {
        return data.get(11);
    }

    /** Сколько клеток круга мела отмечено (этап 2). */
    public int getCircleMarks() {
        return data.get(12);
    }

    /** Сколько клеток нужно для полного круга (88). */
    public int getRequiredCircleMarks() {
        return ChalkCircle.REQUIRED_MARKS;
    }

    /** Сколько из 4 стабилизаторов на месте (этап 3). */
    public int getStabilizerCount() {
        return data.get(13);
    }

    /** Суммарный заряд стабилизаторов (этап 3). */
    public int getStabilizerCharge() {
        return (data.get(15) << 15) | data.get(14);
    }

    public int getStabilizerChargeMax() {
        return getStabilizerCount() * MageStabilizerBlockEntity.MAX_CHARGE;
    }

    /** Шкала стабильности ритуала 0..1000 (1000 — полная). */
    public int getStability() {
        return data.get(16);
    }

    public int getStabilityMax() {
        return SCTBlockEntity.STABILITY_MAX;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (blockEntity.isCrafting()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        if (index == OUTPUT_SLOT) {
            if (player instanceof ServerPlayer serverPlayer) {
                tryStartCraft(serverPlayer);
            }
            return ItemStack.EMPTY;
        }
        ItemStack slotStack = slot.getItem();
        ItemStack original = slotStack.copy();

        boolean moved;
        if (index < PLAYER_SLOT_START) {
            moved = moveItemStackTo(slotStack, PLAYER_SLOT_START, slots.size(), true);
        } else {
            moved = moveItemStackTo(slotStack, 0, INPUT_SLOTS, false)
                    || moveItemStackTo(slotStack, INPUT_SLOTS, INPUT_SLOTS + 2, false);
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (slotStack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    private class SCTCraftingResultSlot extends SlotItemHandler {
        public SCTCraftingResultSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        /** Результат выдаёт {@link SCTBlockEntity} после анимации, ванильное изъятие запрещено. */
        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public ItemStack getItem() {
            if (isClientAnimation() && SCTAnimationState.isHidden(getSlotIndex())) {
                return ItemStack.EMPTY;
            }
            return super.getItem();
        }
    }

    private static class SCTCraftingData implements ContainerData {
        private static final int SLOT_COUNT = 17;

        private final SCTBlockEntity blockEntity;
        private final Player player;
        /** Значения, присланные сервером; используются только на клиенте. */
        private final int[] syncedValues = new int[SLOT_COUNT];

        private SCTCraftingData(SCTBlockEntity blockEntity, Player player) {
            this.blockEntity = blockEntity;
            this.player = player;
        }

        private boolean isClientSide() {
            Level level = blockEntity.getLevel();
            return level != null && level.isClientSide;
        }

        @Override
        public int get(int index) {
            // На клиенте данные Blood Magic и Botania недостоверны (SoulNetwork и пул
            // живут на сервере), поэтому читаем то, что прислал сервер.
            if (isClientSide()) {
                return syncedValues[index];
            }
            ItemStack blood = blockEntity.getInventory().getStackInSlot(BLOOD_SLOT);
            int mana = blockEntity.getManaBuffer();
            int manaCapacity = Math.max(1, blockEntity.getManaCapacity());
            int bloodCapacity = blockEntity.getBloodCapacity();
            // С платформой кровь живёт в буфере машины, без неё — в сфере (как раньше).
            int bloodAmount = bloodCapacity > 0
                    ? blockEntity.getBloodBuffer()
                    : SCTRecipeLogic.getBloodAmount(blood);
            int bloodMax = bloodCapacity > 0
                    ? bloodCapacity
                    : SCTRecipeLogic.getBloodCapacity(blood);
            switch (index) {
                case 0:
                    return bloodMax > 0 ? Math.min(100, bloodAmount * 100 / bloodMax) : 0;
                case 1:
                    return Math.min(100, mana * 100 / manaCapacity);
                case 2:
                    return bloodAmount & 0x7FFF;
                case 3:
                    return (bloodAmount >> 15) & 0x7FFF;
                case 4:
                    return mana & 0x7FFF;
                case 5:
                    return (mana >> 15) & 0x7FFF;
                case 6:
                    return bloodMax & 0x7FFF;
                case 7:
                    return (bloodMax >> 15) & 0x7FFF;
                case 8:
                    return manaCapacity & 0x7FFF;
                case 9:
                    return (manaCapacity >> 15) & 0x7FFF;
                case 10:
                    return blockEntity.isStructureFormed() ? 1 : 0;
                case 11:
                    return blockEntity.getModuleCount();
                case 12:
                    return blockEntity.getCircleMarks();
                case 13:
                    return blockEntity.getStabilizerCount();
                case 14:
                    return blockEntity.getStabilizerCharge() & 0x7FFF;
                case 15:
                    return (blockEntity.getStabilizerCharge() >> 15) & 0x7FFF;
                case 16:
                    return blockEntity.getStability();
                default:
                    return 0;
            }
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < syncedValues.length) {
                syncedValues[index] = value;
            }
        }

        @Override
        public int getCount() {
            return SLOT_COUNT;
        }
    }
}
