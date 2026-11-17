package com.ptdteam.magicadventurecore.world.multiblock;

import com.ptdteam.magicadventurecore.registry.MACBlocks;
import com.ptdteam.magicadventurecore.world.block.SCTModuleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Распознавание платформы SCT: 3×3, центр — контроллер, вокруг кольцо из 8 блоков.
 * Кольцо из 8 блоков: каждая позиция — рамка или модуль, в любом сочетании.
 * Модули дают бонусы, рамки — просто корпус.
 */
public final class SCTStructure {
    /** Смещения кольца вокруг контроллера (по часовой стрелке). */
    public static final int[][] RING_OFFSETS = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1, 0}, {1, 0},
            {-1, 1}, {0, 1}, {1, 1}
    };

    /** Нижний предел ускорения крафта (не быстрее 40% базового времени). */
    public static final float SPEED_FLOOR = 0.40F;

    /**
     * Результат валидации: собрана ли структура и какие бонусы даёт кольцо.
     * {@link #INVALID} — не собрана (или кольцо смешанное).
     */
    public record Info(
            boolean formed,
            int modules,
            int manaBonus,
            int bloodBonus,
            int manaPullBonus,
            int bloodPullBonus,
            float speedMultiplier
    ) {
        public static final Info INVALID = new Info(false, 0, 0, 0, 0, 0, 1.0F);
    }

    private SCTStructure() {
    }

    public static Info validate(Level level, BlockPos controllerPos) {
        int modules = 0;
        int manaBonus = 0;
        int bloodBonus = 0;
        int manaPullBonus = 0;
        int bloodPullBonus = 0;
        float speed = 1.0F;

        for (int[] offset : RING_OFFSETS) {
            BlockPos pos = controllerPos.offset(offset[0], 0, offset[1]);
            if (!level.hasChunkAt(pos)) {
                return Info.INVALID;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(MACBlocks.SCT_FRAME.get())) {
                // Рамка — просто корпус, бонусов не даёт.
                continue;
            }
            if (state.getBlock() instanceof SCTModuleBlock module) {
                modules++;
                manaBonus += module.getManaCapacityBonus();
                bloodBonus += module.getBloodCapacityBonus();
                manaPullBonus += module.getManaPullBonus();
                bloodPullBonus += module.getBloodPullBonus();
                speed *= module.getSpeedMultiplier();
                continue;
            }
            // Любой посторонний блок в кольце ломает структуру.
            return Info.INVALID;
        }

        // Кольцо из 8 блоков: каждая позиция — рамка или модуль, в любом сочетании.
        return new Info(true, modules, manaBonus, bloodBonus,
                manaPullBonus, bloodPullBonus, Math.max(SPEED_FLOOR, speed));
    }
}
