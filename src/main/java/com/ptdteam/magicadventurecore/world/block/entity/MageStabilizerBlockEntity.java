package com.ptdteam.magicadventurecore.world.block.entity;

import com.ptdteam.magicadventurecore.registry.MACBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Магический стабилизатор: копит заряд пассивно и отдаёт его SCT во время ритуального
 * крафта. Чем меньше заряд, тем тусклее кристаллы (это рисует Ber).
 */
public class MageStabilizerBlockEntity extends BlockEntity {
    /** Ёмкость одного стабилизатора. */
    public static final int MAX_CHARGE = 5_000;
    /** Пассивная зарядка: 1 единица в тик (полный — за 250 секунд на блок). */
    public static final int PASSIVE_REGEN_PER_TICK = 1;
    /** Синк клиенту раз в секунду (для затухания кристаллов). */
    private static final int SYNC_INTERVAL = 20;

    private int charge;

    public MageStabilizerBlockEntity(BlockPos pos, BlockState state) {
        super(MACBlockEntities.MAGE_STABILIZER.get(), pos, state);
    }

    public int getCharge() {
        return charge;
    }

    public int getMaxCharge() {
        return MAX_CHARGE;
    }

    /** Доля заряда 0..1 для рендера. */
    public float getChargeFraction() {
        return charge / (float) MAX_CHARGE;
    }

    public int addCharge(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, MAX_CHARGE - charge);
        if (accepted > 0) {
            charge += accepted;
            setChanged();
        }
        return accepted;
    }

    /** Снимает заряд; возвращает сколько реально сняли. */
    public int drainCharge(int amount) {
        int drained = Math.min(Math.max(0, amount), charge);
        if (drained > 0) {
            charge -= drained;
            setChanged();
        }
        return drained;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MageStabilizerBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }
        if (blockEntity.charge < MAX_CHARGE) {
            blockEntity.charge = Math.min(MAX_CHARGE, blockEntity.charge + PASSIVE_REGEN_PER_TICK);
            blockEntity.setChanged();
        }
        if (level.getGameTime() % SYNC_INTERVAL == 0L) {
            blockEntity.sync();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Charge", charge);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        charge = Math.max(0, Math.min(MAX_CHARGE, tag.getInt("Charge")));
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

    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }
}
