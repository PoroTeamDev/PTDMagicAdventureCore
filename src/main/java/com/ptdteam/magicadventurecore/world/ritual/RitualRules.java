package com.ptdteam.magicadventurecore.world.ritual;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Общие правила ритуала: какие блоки считаются «взрывобронированными». */
public final class RitualRules {
    private RitualRules() {
    }

    /**
     * Нельзя разрушить взрывом: бедрок, барьер, обсидиан, эндер-сундук и т.п.
     * Критерий — либо блок вообще неразрушим, либо сопротивление взрыву выше заряда
     * обычного крипера (1200 — как у обсидиана).
     */
    public static boolean isExplosionProof(BlockState state) {
        Block block = state.getBlock();
        if (block.defaultDestroyTime() < 0.0F) {
            return true;
        }
        return block.getExplosionResistance() >= 1200.0F;
    }
}
