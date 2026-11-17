package com.ptdteam.magicadventurecore.world.ritual;

import com.ptdteam.magicadventurecore.registry.MACBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Магический круг мела вокруг SCT.
 *
 * <p>Маска — диск радиусом 5.5 блока (по сетке), из которого вырезана платформа 3×3
 * под самим столом: 88 клеток. Игрок закрашивает их мелом; когда все клетки отмечены,
 * круг считается начерченным и над площадкой «строится» рисунок.</p>
 */
public final class ChalkCircle {
    /** Радиус диска 5.5 → 30.25; для целых клеток это dx² + dz² ≤ 30. */
    private static final int RADIUS_SQ = 30;
    /** Половина стороны платформы 3×3 (клетки |dx| ≤ 1 и |dz| ≤ 1 не требуют мела). */
    private static final int PLATFORM_HALF = 1;

    private static final int[][] OFFSETS = buildOffsets();

    /** Сколько клеток нужно закрасить (88). */
    public static final int REQUIRED_MARKS = OFFSETS.length;

    private ChalkCircle() {
    }

    private static int[][] buildOffsets() {
        int radius = 6;
        int[] temp = new int[(radius * 2 + 1) * (radius * 2 + 1) * 2];
        int count = 0;
        for (int dz = -radius; dz <= radius; dz++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dz * dz > RADIUS_SQ) {
                    continue;
                }
                if (Math.abs(dx) <= PLATFORM_HALF && Math.abs(dz) <= PLATFORM_HALF) {
                    continue;
                }
                temp[count++] = dx;
                temp[count++] = dz;
            }
        }
        int[][] offsets = new int[count / 2][2];
        for (int i = 0; i < offsets.length; i++) {
            offsets[i][0] = temp[i * 2];
            offsets[i][1] = temp[i * 2 + 1];
        }
        return offsets;
    }

    /** Смещения клеток круга относительно контроллера (только чтение). */
    public static int[][] offsets() {
        return OFFSETS.clone();
    }

    public record Info(int marks, int required) {
        public static final Info EMPTY = new Info(0, REQUIRED_MARKS);

        public boolean complete() {
            return marks >= required;
        }
    }

    /** Считает, сколько клеток маски закрашено мелом. */
    public static Info validate(Level level, BlockPos controllerPos) {
        int marks = 0;
        for (int[] offset : OFFSETS) {
            BlockPos pos = controllerPos.offset(offset[0], 0, offset[1]);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            if (level.getBlockState(pos).is(MACBlocks.CHALK_MARK.get())) {
                marks++;
            }
        }
        return new Info(marks, REQUIRED_MARKS);
    }
}
