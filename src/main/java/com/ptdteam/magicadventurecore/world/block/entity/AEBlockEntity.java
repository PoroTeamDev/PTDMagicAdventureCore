package com.ptdteam.magicadventurecore.world.block.entity;

import com.ptdteam.magicadventurecore.registry.MACBlockEntities;
import com.ptdteam.magicadventurecore.world.essence.ArcanaSources;
import com.ptdteam.magicadventurecore.world.essence.EssenceType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
    private static final int EXTRACT_PER_TICK = 100;
    private static final int TRANSFER_PER_TICK = 500;

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            ItemStack stack = getStackInSlot(slot);
            remainingArcana = stack.isEmpty() ? 0 : ArcanaSources.getArcanaValue(stack);
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
    };
    private final LazyOptional<ItemStackHandler> inventoryOptional = LazyOptional.of(() -> inventory);

    private int arcanaBuffer = 0;
    private int remainingArcana = 0;

    public AEBlockEntity(BlockPos pos, BlockState state) {
        super(MACBlockEntities.ARCANE_EXTRACTOR.get(), pos, state);
    }

    public ItemStack getDisplayItem() {
        return inventory.getStackInSlot(0);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

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
            remainingArcana = ArcanaSources.getArcanaValue(stack);
            if (remainingArcana <= 0) {
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
        if (remainingArcana <= 0) {
            inventory.setStackInSlot(0, ItemStack.EMPTY);
        }
        sync();
        return true;
    }

    private boolean transferArcana() {
        if (arcanaBuffer <= 0 || level == null) {
            return false;
        }
        int remainingToTransfer = Math.min(TRANSFER_PER_TICK, arcanaBuffer);
        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(direction));
            if (neighbor instanceof ERBlockEntity reservoir) {
                int accepted = reservoir.addEssence(EssenceType.ARCANA, remainingToTransfer);
                if (accepted > 0) {
                    arcanaBuffer -= accepted;
                    sync();
                    return true;
                }
            }
        }
        return false;
    }

    public void dropContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        ItemStack stack = inventory.getStackInSlot(0);
        if (!stack.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            inventory.setStackInSlot(0, ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", inventory.serializeNBT());
        tag.putInt("ArcanaBuffer", arcanaBuffer);
        tag.putInt("RemainingArcana", remainingArcana);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        arcanaBuffer = tag.getInt("ArcanaBuffer");
        remainingArcana = tag.getInt("RemainingArcana");
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