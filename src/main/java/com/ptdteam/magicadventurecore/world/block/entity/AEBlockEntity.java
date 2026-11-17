package com.ptdteam.magicadventurecore.world.block.entity;

import com.ptdteam.magicadventurecore.registry.MACBlockEntities;
import com.ptdteam.magicadventurecore.registry.MACBlocks;
import com.ptdteam.magicadventurecore.world.essence.ArcanaSources;
import com.ptdteam.magicadventurecore.world.essence.EssenceType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class AEBlockEntity extends BlockEntity {
    public static final int MAX_ARCANA = 10000;
    /** Переработка замедлена в 50 раз относительно прежней скорости (было 100/тик). */
    private static final int EXTRACT_PER_TICK = 2;
    private static final int TRANSFER_PER_TICK = 500;
    /** Радиус поиска ближайшего резервуара эссенции. */
    private static final int RESERVOIR_SEARCH_RADIUS = 16;
    /** Как часто (в тиках) отправлять клиенту прогресс для анимации. */
    private static final int PROGRESS_SYNC_INTERVAL = 10;
    /** Сколько арканы уже выжато из предмета в слоте. Пишется в NBT предмета. */
    public static final String EXTRACTED_TAG = "ArcanaExtracted";

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            // Остаток всегда выводится из самого предмета (а не хранится отдельно),
            // иначе вынуть/вставить частично переработанный предмет давало новый запас арканы.
            remainingArcana = computeRemaining(getStackInSlot(slot));
            progressSync = computeProgressScaled();
            findNearestReservoir();
            setChanged();
            sync();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ArcanaSources.isArcanaSource(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Автоматика/игрок может вынуть предмет в любой момент: сохраняем на нём прогресс,
            // чтобы после повторной вставки переработка продолжилась с того же места.
            int remainingBefore = remainingArcana;
            ItemStack extracted = super.extractItem(slot, amount, simulate);
            if (!simulate && !extracted.isEmpty()) {
                int total = ArcanaSources.getArcanaValue(extracted);
                int extractedSoFar = Math.max(0, total - remainingBefore);
                if (extractedSoFar > 0) {
                    extracted.getOrCreateTag().putInt(EXTRACTED_TAG, extractedSoFar);
                }
            }
            return extracted;
        }
    };
    private final LazyOptional<ItemStackHandler> inventoryOptional = LazyOptional.of(() -> inventory);

    private int arcanaBuffer = 0;
    private int remainingArcana = 0;
    /** Прогресс текущего предмета 0..1000 — синхронизируется клиенту для анимации. */
    private int progressSync = 0;
    private int syncTimer = 0;
    /** Ближайший резервуар, к которому летят частицы и куда сливается аркана. */
    @Nullable
    private BlockPos linkedReservoir;

    public AEBlockEntity(BlockPos pos, BlockState state) {
        super(MACBlockEntities.ARCANE_EXTRACTOR.get(), pos, state);
    }

    public ItemStack getDisplayItem() {
        return inventory.getStackInSlot(0);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    // ------------------------------------------------------------- анимация

    /** Прогресс переработки 0..1 (синхронизированная копия для рендера). */
    public float getClientProgress() {
        return progressSync / 1000.0F;
    }

    @Nullable
    public BlockPos getLinkedReservoir() {
        return linkedReservoir;
    }

    // --------------------------------------------------------------- логика

    public static void tick(Level level, BlockPos pos, BlockState state, AEBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        boolean changed = false;
        changed |= blockEntity.extractArcana();
        changed |= blockEntity.transferArcana();
        if (changed) {
            blockEntity.setChanged();
        }
        // Периодический синк прогресса — клиенту нужен он для вращения и партиклов.
        if (blockEntity.syncTimer++ >= PROGRESS_SYNC_INTERVAL) {
            blockEntity.syncTimer = 0;
            int scaled = blockEntity.computeProgressScaled();
            if (scaled != blockEntity.progressSync) {
                blockEntity.progressSync = scaled;
                blockEntity.sync();
            }
        }
    }

    private int computeProgressScaled() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) {
            return 0;
        }
        int total = ArcanaSources.getArcanaValue(stack);
        if (total <= 0) {
            return 0;
        }
        int done = Mth.clamp(total - remainingArcana, 0, total);
        return Mth.clamp(done * 1000 / total, 0, 1000);
    }

    /** Сколько арканы ещё можно получить из предмета с учётом записанного в нём прогресса. */
    private static int computeRemaining(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int total = ArcanaSources.getArcanaValue(stack);
        if (total <= 0) {
            return 0;
        }
        int alreadyExtracted = stack.hasTag() ? stack.getTag().getInt(EXTRACTED_TAG) : 0;
        return Math.max(0, total - alreadyExtracted);
    }

    private void findNearestReservoir() {
        if (level == null || level.isClientSide) {
            linkedReservoir = null;
            return;
        }
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos from = worldPosition.offset(-RESERVOIR_SEARCH_RADIUS, -RESERVOIR_SEARCH_RADIUS, -RESERVOIR_SEARCH_RADIUS);
        BlockPos to = worldPosition.offset(RESERVOIR_SEARCH_RADIUS, RESERVOIR_SEARCH_RADIUS, RESERVOIR_SEARCH_RADIUS);
        for (BlockPos candidate : BlockPos.betweenClosed(from, to)) {
            if (!level.getBlockState(candidate).is(MACBlocks.ESSENCE_RESERVOIR.get())) {
                continue;
            }
            double distance = candidate.distSqr(worldPosition);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate.immutable();
            }
        }
        linkedReservoir = best;
    }

    private boolean extractArcana() {
        if (arcanaBuffer >= MAX_ARCANA) {
            return false;
        }
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty()) {
            return false;
        }
        if (remainingArcana <= 0) {
            remainingArcana = computeRemaining(stack);
            if (remainingArcana <= 0) {
                // Предмет полностью переработан (или вообще не источник) — убираем его.
                inventory.setStackInSlot(0, ItemStack.EMPTY);
                return true;
            }
        }
        int space = MAX_ARCANA - arcanaBuffer;
        int extracted = Math.min(EXTRACT_PER_TICK, Math.min(space, remainingArcana));
        if (extracted <= 0) {
            return false;
        }
        arcanaBuffer += extracted;
        remainingArcana -= extracted;

        // Прогресс пишем в NBT предмета напрямую, без onContentsChanged: иначе каждый тик
        // уходил бы пакет синхронизации блок-сущности.
        int total = ArcanaSources.getArcanaValue(stack);
        stack.getOrCreateTag().putInt(EXTRACTED_TAG, total - remainingArcana);
        if (remainingArcana <= 0) {
            inventory.setStackInSlot(0, ItemStack.EMPTY);
        }
        return true;
    }

    private boolean transferArcana() {
        if (arcanaBuffer <= 0 || level == null) {
            return false;
        }
        int remainingToTransfer = Math.min(TRANSFER_PER_TICK, arcanaBuffer);

        // Сначала пробуем ближайший найденный резервуар, затем соседей.
        if (linkedReservoir != null) {
            if (level.getBlockEntity(linkedReservoir) instanceof ERBlockEntity reservoir) {
                int accepted = reservoir.addEssence(EssenceType.ARCANA, remainingToTransfer);
                if (accepted > 0) {
                    arcanaBuffer -= accepted;
                    sync();
                    return true;
                }
            } else {
                linkedReservoir = null;
            }
        }
        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor instanceof ERBlockEntity reservoir) {
                int accepted = reservoir.addEssence(EssenceType.ARCANA, remainingToTransfer);
                if (accepted > 0) {
                    arcanaBuffer -= accepted;
                    linkedReservoir = reservoir.getBlockPos();
                    sync();
                    return true;
                }
            }
        }
        return false;
    }

    /** Возвращает содержимое слота в мир (вызывается при разрушении блока). */
    public void dropContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            inventory.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("ArcanaBuffer", arcanaBuffer);
        tag.putInt("Progress", progressSync);
        if (linkedReservoir != null) {
            tag.put("LinkedReservoir", NbtUtils.writeBlockPos(linkedReservoir));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        arcanaBuffer = tag.getInt("ArcanaBuffer");
        progressSync = tag.getInt("Progress");
        linkedReservoir = tag.contains("LinkedReservoir")
                ? NbtUtils.readBlockPos(tag.getCompound("LinkedReservoir"))
                : null;
        // Прогресс хранится в NBT предмета, поэтому после загрузки остаток пересчитывается,
        // а не читается из блочной сущности (старые сохранения поле RemainingArcana игнорируют).
        remainingArcana = computeRemaining(inventory.getStackInSlot(0));
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryOptional.invalidate();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryOptional.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(1.0);
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}
