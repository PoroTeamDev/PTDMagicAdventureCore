package com.ptdteam.magicadventurecore.world.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Функциональный модуль кольца SCT. Кольцо из 8 модулей (типы можно смешивать)
 * даёт столу бонусы к буферам и скорости крафта.
 */
public class SCTModuleBlock extends Block {
    public enum Type {
        /** Расширяет буфер маны и ускоряет забор маны из мана-предмета. */
        MANA_RESERVOIR(250_000, 0, 5_000, 0, 1.0F),
        /** Расширяет буфер крови и ускоряет забор LP из сферы. */
        BLOOD_RESERVOIR(0, 10_000, 0, 500, 1.0F),
        /** Ускоряет крафт (×0.90 за блок, пол ×0.40). */
        ACCELERATOR(0, 0, 0, 0, 0.90F);

        private final int manaCapacityBonus;
        private final int bloodCapacityBonus;
        private final int manaPullBonus;
        private final int bloodPullBonus;
        private final float speedMultiplier;

        Type(int manaCapacityBonus, int bloodCapacityBonus, int manaPullBonus,
             int bloodPullBonus, float speedMultiplier) {
            this.manaCapacityBonus = manaCapacityBonus;
            this.bloodCapacityBonus = bloodCapacityBonus;
            this.manaPullBonus = manaPullBonus;
            this.bloodPullBonus = bloodPullBonus;
            this.speedMultiplier = speedMultiplier;
        }
    }

    private final Type type;

    public SCTModuleBlock(BlockBehaviour.Properties properties, Type type) {
        super(properties);
        this.type = type;
    }

    public Type getModuleType() {
        return type;
    }

    public int getManaCapacityBonus() {
        return type.manaCapacityBonus;
    }

    public int getBloodCapacityBonus() {
        return type.bloodCapacityBonus;
    }

    public int getManaPullBonus() {
        return type.manaPullBonus;
    }

    public int getBloodPullBonus() {
        return type.bloodPullBonus;
    }

    public float getSpeedMultiplier() {
        return type.speedMultiplier;
    }
}
