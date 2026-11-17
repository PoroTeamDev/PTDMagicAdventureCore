package com.ptdteam.magicadventurecore.client;

import com.ptdteam.magicadventurecore.MagicAdventureCore;
import com.ptdteam.magicadventurecore.registry.MACBlocks;
import com.ptdteam.magicadventurecore.world.block.entity.SCTBlockEntity;
import com.ptdteam.magicadventurecore.world.item.ArcaneChalkItem;
import com.ptdteam.magicadventurecore.world.ritual.ChalkCircle;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Призрачный контур круга: пока игрок держит мел рядом с SCT, в незакрашенных клетках
 * маски появляются тусклые искры — видно, где ещё нужно провести мелом.
 */
@Mod.EventBusSubscriber(modid = MagicAdventureCore.MOD_ID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ChalkGhostHandler {
    private static final int SEARCH_RADIUS = 12;
    private static final int SEARCH_VERTICAL = 3;

    private static int tickCounter;
    private static int cacheTimer;
    private static BlockPos cachedController;

    private ChalkGhostHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }
        if (!isChalk(minecraft.player.getMainHandItem())
                && !isChalk(minecraft.player.getOffhandItem())) {
            cachedController = null;
            return;
        }
        if (tickCounter++ % 2 != 0) {
            return;
        }
        BlockPos controller = resolveController(level, minecraft.player.blockPosition());
        if (controller == null) {
            return;
        }
        if (!(level.getBlockEntity(controller) instanceof SCTBlockEntity blockEntity)
                || blockEntity.isCircleComplete()) {
            return;
        }
        List<BlockPos> missing = new ArrayList<>();
        for (int[] offset : ChalkCircle.offsets()) {
            BlockPos pos = controller.offset(offset[0], 0, offset[1]);
            if (!level.getBlockState(pos).is(MACBlocks.CHALK_MARK.get())) {
                missing.add(pos);
            }
        }
        if (missing.isEmpty()) {
            return;
        }
        int shown = Math.min(4, missing.size());
        for (int i = 0; i < shown; i++) {
            BlockPos pos = missing.get((tickCounter + i * missing.size() / shown) % missing.size());
            level.addParticle(ParticleTypes.END_ROD,
                    pos.getX() + 0.5D, pos.getY() + 0.07D, pos.getZ() + 0.5D,
                    0.0D, 0.0D, 0.0D);
        }
    }

    private static boolean isChalk(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ArcaneChalkItem;
    }

    private static BlockPos resolveController(Level level, BlockPos playerPos) {
        if (cacheTimer-- <= 0 || cachedController == null
                || !level.getBlockState(cachedController).is(MACBlocks.SCT.get())) {
            cachedController = findController(level, playerPos);
            cacheTimer = 20;
        }
        return cachedController;
    }

    private static BlockPos findController(Level level, BlockPos origin) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dy = -SEARCH_VERTICAL; dy <= SEARCH_VERTICAL; dy++) {
            for (int dz = -SEARCH_RADIUS; dz <= SEARCH_RADIUS; dz++) {
                for (int dx = -SEARCH_RADIUS; dx <= SEARCH_RADIUS; dx++) {
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.hasChunkAt(cursor)) {
                        continue;
                    }
                    if (!level.getBlockState(cursor).is(MACBlocks.SCT.get())) {
                        continue;
                    }
                    double dx0 = cursor.getX() - origin.getX();
                    double dy0 = cursor.getY() - origin.getY();
                    double dz0 = cursor.getZ() - origin.getZ();
                    double distance = dx0 * dx0 + dy0 * dy0 + dz0 * dz0;
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = cursor.immutable();
                    }
                }
            }
        }
        return best;
    }
}
