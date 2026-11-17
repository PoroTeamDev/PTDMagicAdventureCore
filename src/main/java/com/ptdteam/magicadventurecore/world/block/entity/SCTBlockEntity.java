package com.ptdteam.magicadventurecore.world.block.entity;

import com.ptdteam.magicadventurecore.registry.MACBlockEntities;
import com.ptdteam.magicadventurecore.world.menu.SCTCraftingMenu;
import com.ptdteam.magicadventurecore.world.recipe.SCTHybridRecipe;
import com.ptdteam.magicadventurecore.world.recipe.SCTRecipeLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.BotaniaForgeCapabilities;
import vazkii.botania.api.mana.ManaItem;
import vazkii.botania.api.mana.ManaReceiver;
import vazkii.botania.api.mana.spark.ManaSpark;
import vazkii.botania.api.mana.spark.SparkAttachable;
import vazkii.botania.common.entity.ManaSparkEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class SCTBlockEntity extends BlockEntity implements MenuProvider, ManaReceiver, SparkAttachable {
    /** Индексы слотов инвентаря: 0..13 — сетка, 14 — кровь, 15 — мана, 16 — выход. */
    public static final int INPUT_SLOTS = 14;
    public static final int BLOOD_SLOT = 14;
    public static final int MANA_SLOT = 15;
    public static final int OUTPUT_SLOT = 16;
    public static final int TOTAL_SLOTS = 17;

    /**
     * Внутренний буфер маны: сюда сливают искры Botania и мана-предметы из слота.
     * Голый стол (без кольца 3×3) — 5 000. С кольцом рамок будет 50 000, модули добавят
     * сверху; ёмкость станет динамической на этапе мультиблока.
     */
    public static final int MAX_MANA = 5_000;
    /** Сколько маны в тик забираем из мана-предмета в буфер. */
    public static final int MANA_PULL_PER_TICK = 5000;
    /** Скорость перекачки ресурсов в анимации: замедлена в 3 раза (было 1000 единиц в секунду). */
    public static final int RESOURCE_PER_SECOND = 320;
    public static final int RESOURCE_PER_TICK = RESOURCE_PER_SECOND / 20;

    /**
     * Сырой инвентарь. Меню работает именно с ним: слоты Forge-а читают и вынимают
     * предметы через {@link ItemStackHandler#extractItem}, поэтому ограничивать его
     * нельзя — иначе перестаёт выдаваться результат крафта.
     */
    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            sync();
        }
    };

    /**
     * Для автоматики (трубы, хопперы, другие моды) отдаём только слоты 0..15:
     * выход в неё не попадает и не может быть вынут без списания ингредиентов.
     * Во время анимации автоматика заблокирована полностью.
     */
    private final LazyOptional<IItemHandler> automationHandler = LazyOptional.of(() -> new RangedWrapper(inventory, 0, OUTPUT_SLOT) {
        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (crafting) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (crafting) {
                return ItemStack.EMPTY;
            }
            return super.extractItem(slot, amount, simulate);
        }
    });

    private final LazyOptional<ManaReceiver> manaReceiver = LazyOptional.of(() -> this);
    private final LazyOptional<SparkAttachable> sparkAttachable = LazyOptional.of(() -> this);

    /** Внутренний буфер маны (0..MAX_MANA). */
    private int manaBuffer;

    /** Идёт ли сейчас анимация крафта (слоты заблокированы, ингредиенты ещё не списаны). */
    private boolean crafting;
    private long craftStartGameTime;
    /** Длительность фазы перекачки крови/маны в тиках (1000 единиц ресурса в секунду). */
    private int craftResourceTicks;
    /** Длительность фазы поглощения ингредиентов в тиках. */
    private int craftDuration;
    /** Стоимости текущего крафта — синхронизируются клиенту для анимации потока. */
    private int craftBloodCost;
    private int craftManaCost;
    @Nullable
    private UUID craftOwner;

    public SCTBlockEntity(BlockPos pos, BlockState state) {
        super(MACBlockEntities.SCT_BLOCK_ENTITY.get(), pos, state);
    }

    /** Полный инвентарь для меню и внутренней логики. */
    public ItemStackHandler getInventory() {
        return inventory;
    }

    // ------------------------------------------------------------ мана-буфер

    public int getManaBuffer() {
        return manaBuffer;
    }

    /** Добавляет ману в буфер, возвращает сколько реально влезло. */
    public int addMana(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, MAX_MANA - manaBuffer);
        if (accepted <= 0) {
            return 0;
        }
        manaBuffer += accepted;
        setChanged();
        return accepted;
    }

    public int drainMana(int amount) {
        int drained = Math.min(Math.max(0, amount), manaBuffer);
        if (drained > 0) {
            manaBuffer -= drained;
            setChanged();
        }
        return drained;
    }

    /**
     * Тянет ману из мана-предмета в слоте (кольца, планшеты, зеркало — всё, что даёт
     * Botania-капабилити MANA_ITEM) во внутренний буфер. Игрок не нужен.
     */
    private void pullManaFromSlot() {
        if (manaBuffer >= MAX_MANA) {
            return;
        }
        ItemStack stack = inventory.getStackInSlot(MANA_SLOT);
        if (stack.isEmpty()) {
            return;
        }
        Optional<ManaItem> manaItem = stack.getCapability(BotaniaForgeCapabilities.MANA_ITEM).resolve();
        if (manaItem.isEmpty()) {
            return;
        }
        ManaItem item = manaItem.get();
        int room = MAX_MANA - manaBuffer;
        int available = Math.min(item.getMana(), Math.min(room, MANA_PULL_PER_TICK));
        if (available <= 0) {
            return;
        }
        item.addMana(-available);
        manaBuffer += available;
        setChanged();
    }

    // ---------------------------------------------------- ManaReceiver (Botania)

    @Override
    public void receiveMana(int mana) {
        addMana(mana);
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return true;
    }

    @Override
    public boolean isFull() {
        return manaBuffer >= MAX_MANA;
    }

    @Override
    public int getCurrentMana() {
        return manaBuffer;
    }

    @Override
    public Level getManaReceiverLevel() {
        return getLevel();
    }

    @Override
    public BlockPos getManaReceiverPos() {
        return getBlockPos();
    }

    // --------------------------------------------------- SparkAttachable (Botania)

    @Override
    public boolean canAttachSpark(ItemStack stack) {
        return getAttachedSpark() == null;
    }

    @Nullable
    @Override
    public ManaSpark getAttachedSpark() {
        Level level = getLevel();
        if (level == null) {
            return null;
        }
        List<ManaSparkEntity> sparks = level.getEntitiesOfClass(
                ManaSparkEntity.class, new AABB(worldPosition).inflate(1.5D));
        for (ManaSparkEntity spark : sparks) {
            if (worldPosition.equals(spark.getAttachPos())) {
                return spark;
            }
        }
        return null;
    }

    @Override
    public boolean areIncomingTranfersDone() {
        return manaBuffer >= MAX_MANA;
    }

    @Override
    public int getAvailableSpaceForMana() {
        return Math.max(0, MAX_MANA - manaBuffer);
    }

    // ------------------------------------------------------------- анимация

    public boolean isCrafting() {
        return crafting;
    }

    public long getCraftStartGameTime() {
        return craftStartGameTime;
    }

    public int getCraftDuration() {
        return craftDuration;
    }

    public int getCraftResourceTicks() {
        return craftResourceTicks;
    }

    /** Стоимость крови текущего крафта (синхронизируется клиенту для анимации потока). */
    public int getCraftBloodCost() {
        return craftBloodCost;
    }

    /** Стоимость маны текущего крафта (синхронизируется клиенту для анимации потока). */
    public int getCraftManaCost() {
        return craftManaCost;
    }

    /** Хватает ли ресурсов на крафт (кровь + мана с учётом буфера). */
    public boolean hasEnoughResources(SCTHybridRecipe recipe, @Nullable Player player) {
        Level level = getLevel();
        if (!SCTRecipeLogic.hasEnoughBlood(inventory.getStackInSlot(BLOOD_SLOT), recipe.getBloodCost())) {
            return false;
        }
        int remainingMana = recipe.getManaCost() - manaBuffer;
        if (remainingMana <= 0) {
            return true;
        }
        return SCTRecipeLogic.hasEnoughManaItem(
                inventory.getStackInSlot(MANA_SLOT), player, level, remainingMana);
    }

    /** Списывает ингредиенты, кровь и ману (сначала буфер, остаток — из мана-предмета/пула). */
    private void consumeResources(SCTHybridRecipe recipe, @Nullable Player player) {
        Level level = getLevel();
        SCTRecipeLogic.consumeIngredients(recipe, inventory);
        SCTRecipeLogic.consumeBlood(inventory.getStackInSlot(BLOOD_SLOT), recipe.getBloodCost());
        int remaining = recipe.getManaCost() - drainMana(recipe.getManaCost());
        if (remaining > 0) {
            SCTRecipeLogic.consumeManaItem(inventory.getStackInSlot(MANA_SLOT), player, level, remaining);
        }
    }

    /**
     * Запускает анимацию крафта. Ингредиенты и ресурсы списываются только в конце,
     * поэтому выход из игры или поломка блока посреди анимации ничего не теряют.
     */
    public boolean startCraft(ServerPlayer player) {
        Level level = getLevel();
        if (level == null || level.isClientSide || crafting) {
            return false;
        }
        if (inventory.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            return false;
        }
        Optional<SCTHybridRecipe> recipe = SCTRecipeLogic.findRecipe(level, inventory);
        if (recipe.isEmpty()) {
            return false;
        }
        if (!hasEnoughResources(recipe.get(), player)) {
            return false;
        }
        this.crafting = true;
        this.craftStartGameTime = level.getGameTime();
        this.craftDuration = recipe.get().getCraftTime() * 3;
        this.craftBloodCost = recipe.get().getBloodCost();
        this.craftManaCost = recipe.get().getManaCost();
        // Кровь и мана "перекачиваются" со скоростью 320 единиц в секунду: чем дороже
        // рецепт, тем дольше вступительная фаза (поток партиклов от боковых шкал).
        int resourceAmount = Math.max(craftBloodCost, craftManaCost);
        this.craftResourceTicks = Math.max(0, resourceAmount / RESOURCE_PER_TICK);
        this.craftOwner = player.getUUID();
        if (craftDuration <= 0 && craftResourceTicks <= 0) {
            finishCraft();
            return true;
        }
        setChanged();
        sync();
        return true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, SCTBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        blockEntity.serverTick();
    }

    private void serverTick() {
        pullManaFromSlot();
        if (!crafting) {
            return;
        }
        Level level = getLevel();
        if (level == null) {
            resetCraft();
            return;
        }
        if (level.getGameTime() - craftStartGameTime >= (long) craftResourceTicks + craftDuration) {
            finishCraft();
        }
    }

    private void finishCraft() {
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            resetCraft();
            return;
        }
        ItemStack result = inventory.getStackInSlot(OUTPUT_SLOT).copy();
        Optional<SCTHybridRecipe> recipe = SCTRecipeLogic.findRecipe(level, inventory);
        ServerPlayer owner = resolveOwner();
        boolean valid = recipe.isPresent()
                && !result.isEmpty()
                && hasEnoughResources(recipe.get(), owner);
        if (!valid) {
            // Ресурсы могли измениться за время анимации — отменяем без потерь.
            resetCraft();
            if (owner != null) {
                owner.displayClientMessage(
                        Component.translatable("message.magicadventurecore.craft_aborted"), true);
            }
            sync();
            return;
        }
        consumeResources(recipe.get(), owner);
        resetCraft();
        // Списанные ингредиенты могли оставить в сетке другой валидный рецепт — пересчитываем.
        SCTRecipeLogic.refreshOutput(level, inventory);
        SCTRecipeLogic.deliver(result, owner, level, worldPosition);
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.9F, 1.2F);
        setChanged();
        sync();
    }

    /** Отмена анимации без списания ресурсов (например, блок сломали). */
    public void cancelCraft() {
        if (crafting) {
            resetCraft();
            sync();
        }
    }

    @Nullable
    private ServerPlayer resolveOwner() {
        if (craftOwner == null) {
            return null;
        }
        MinecraftServer server = getLevel() != null ? getLevel().getServer() : null;
        if (server == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(craftOwner);
    }

    private void resetCraft() {
        this.crafting = false;
        this.craftStartGameTime = 0L;
        this.craftDuration = 0;
        this.craftResourceTicks = 0;
        this.craftBloodCost = 0;
        this.craftManaCost = 0;
        this.craftOwner = null;
        setChanged();
    }

    // ------------------------------------------------------- BlockEntity API

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.magicadventurecore.sct_crafting");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SCTCraftingMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("ManaBuffer", manaBuffer);
        tag.putBoolean("Crafting", crafting);
        tag.putLong("CraftStart", craftStartGameTime);
        tag.putInt("CraftResource", craftResourceTicks);
        tag.putInt("CraftDuration", craftDuration);
        tag.putInt("CraftBloodCost", craftBloodCost);
        tag.putInt("CraftManaCost", craftManaCost);
        if (craftOwner != null) {
            tag.putString("CraftOwner", craftOwner.toString());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        manaBuffer = Math.max(0, Math.min(MAX_MANA, tag.getInt("ManaBuffer")));
        crafting = tag.getBoolean("Crafting");
        craftStartGameTime = tag.getLong("CraftStart");
        craftResourceTicks = tag.getInt("CraftResource");
        craftDuration = tag.getInt("CraftDuration");
        craftBloodCost = tag.getInt("CraftBloodCost");
        craftManaCost = tag.getInt("CraftManaCost");
        craftOwner = tag.contains("CraftOwner") ? parseUuid(tag.getString("CraftOwner")) : null;
        if (crafting && craftDuration <= 0 && craftResourceTicks <= 0) {
            crafting = false;
        }
    }

    @Nullable
    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Клиентский {@link SCTCraftingMenu} разрешает блок-сущность через {@code level.getBlockEntity(pos)},
     * поэтому инвентарь и состояние крафта обязаны уезжать на клиент.
     */
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        // saveWithoutMetadata уже содержит Inventory и поля анимации из saveAdditional.
        return saveWithoutMetadata();
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return automationHandler.cast();
        }
        if (capability == BotaniaForgeCapabilities.MANA_RECEIVER) {
            return manaReceiver.cast();
        }
        if (capability == BotaniaForgeCapabilities.SPARK_ATTACHABLE) {
            return sparkAttachable.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        automationHandler.invalidate();
        manaReceiver.invalidate();
        sparkAttachable.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
    }
}
