package com.ptdteam.magicadventurecore.world.block.entity;

import com.ptdteam.magicadventurecore.registry.MACBlockEntities;
import com.ptdteam.magicadventurecore.world.block.ERBlock;

import com.ptdteam.magicadventurecore.world.essence.EssenceStorage;
import com.ptdteam.magicadventurecore.world.essence.EssenceType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import vazkii.botania.api.mana.ManaReceiver;

public class ERBlockEntity extends BlockEntity implements ManaReceiver {
    public static final int MAX_ESSENCE = 100000;

    private EssenceType essenceType = EssenceType.NONE;
    private int essenceAmount = 0;

    public ERBlockEntity(BlockPos pos, BlockState state) {
        super(MACBlockEntities.ESSENCE_RESERVOIR.get(), pos, state);
    }

    public EssenceType getEssenceType() {
        return essenceType;
    }

    public int getEssenceAmount() {
        return essenceAmount;
    }

    public int addEssence(EssenceType type, int amount) {
        if (amount <= 0) {
            return 0;
        }
        if (!EssenceStorage.canAccept(essenceType, type)) {
            return 0;
        }
        int accepted = Math.min(amount, MAX_ESSENCE - essenceAmount);
        if (accepted <= 0) {
            return 0;
        }
        essenceAmount += accepted;
        if (essenceType == EssenceType.NONE) {
            essenceType = type;
        }
        setChanged();
        updateFillLevel();
        sync();
        return accepted;
    }

    public void setEssence(EssenceType type, int amount) {
        if (amount <= 0 || type == EssenceType.NONE) {
            essenceType = EssenceType.NONE;
            essenceAmount = 0;
        } else {
            essenceType = type;
            essenceAmount = Math.min(amount, MAX_ESSENCE);
        }
        setChanged();
        updateFillLevel();
        sync();
    }

    public void loadFromItem(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        EssenceType type = EssenceStorage.getEssenceType(tag);
        int amount = EssenceStorage.getEssenceAmount(tag);
        setEssence(type, amount);
    }

    public void saveToItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        EssenceStorage.writeToStack(stack, essenceType, essenceAmount);
    }

    private void updateFillLevel() {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.hasProperty(ERBlock.LEVEL)) {
            return;
        }
        int desired = getFillLevel();
        if (state.getValue(ERBlock.LEVEL) != desired) {
            level.setBlock(worldPosition, state.setValue(ERBlock.LEVEL, desired), 3);
        }
    }

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private int getFillLevel() {
        if (essenceAmount <= 0) {
            return 0;
        }
        return Mth.clamp(Mth.ceil(essenceAmount * 4F / MAX_ESSENCE), 1, 4);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        EssenceStorage.writeToTag(tag, essenceType, essenceAmount);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        essenceType = EssenceStorage.getEssenceType(tag);
        essenceAmount = EssenceStorage.getEssenceAmount(tag);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            updateFillLevel();
        }
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
    public void receiveMana(int mana) {
        addEssence(EssenceType.MANA, mana);
    }

    @Override
    public Level getManaReceiverLevel() {
        return level;
    }

    @Override
    public BlockPos getManaReceiverPos() {
        return worldPosition;
    }

    @Override
    public int getCurrentMana() {
        return essenceType == EssenceType.MANA ? essenceAmount : 0;
    }

    @Override
    public boolean isFull() {
        if (essenceType != EssenceType.NONE && essenceType != EssenceType.MANA) {
            return true;
        }
        return essenceAmount >= MAX_ESSENCE;
    }

    @Override
    public boolean canReceiveManaFromBursts() {
        return true;
    }
}