package com.ptdteam.magicadventurecore.world.item;

import com.ptdteam.magicadventurecore.registry.MACBlocks;
import com.ptdteam.magicadventurecore.world.block.entity.ERBlockEntity;
import com.ptdteam.magicadventurecore.world.essence.EssenceType;
import com.ptdteam.magicadventurecore.world.particle.LifeOrbParticleData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Гем Жизни: переносит эссенцию жизни (до 16 000).
 * - Заряжается убийствами мобов (см. EREvents), 8 визуальных стадий.
 * - У резервуара: зажать ПКМ гемом — к урне летят красные орбы, гем разряжается,
 *   резервуар наполняется эссенцией жизни.
 */
public class GemOfLifeItem extends Item {
    public static final int MAX_ESSENCE = 16000;
    /** Сколько эссенции переливается в резервуар за тик удержания. */
    public static final int POUR_PER_TICK = 500;
    /** Радиус поиска резервуара при переливании. */
    public static final int POUR_RADIUS = 6;

    private static final String ESSENCE_TAG = "LifeEssence";

    public GemOfLifeItem(Properties properties) {
        super(properties);
    }

    public static int getEssence(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) {
            return 0;
        }
        return stack.getTag().getInt(ESSENCE_TAG);
    }

    public static void setEssence(ItemStack stack, int amount) {
        if (stack.isEmpty()) {
            return;
        }
        int clamped = Math.max(0, Math.min(MAX_ESSENCE, amount));
        CompoundTag tag = stack.getOrCreateTag();
        if (clamped <= 0) {
            tag.remove(ESSENCE_TAG);
        } else {
            tag.putInt(ESSENCE_TAG, clamped);
        }
    }

    /** Добавляет эссенцию в гем, возвращает сколько реально влезло. */
    public static int addEssence(ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) {
            return 0;
        }
        int current = getEssence(stack);
        int accepted = Math.min(amount, MAX_ESSENCE - current);
        if (accepted > 0) {
            setEssence(stack, current + accepted);
        }
        return accepted;
    }

    /** Доля заполнения 0..1 — для модели (predicate fill) и тултипа. */
    public static float getFill(ItemStack stack) {
        return getEssence(stack) / (float) MAX_ESSENCE;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        int essence = getEssence(stack);
        tooltip.add(Component.translatable("tooltip.magicadventurecore.gem_of_life.amount",
                essence, MAX_ESSENCE).withStyle(ChatFormatting.RED));
        if (essence > 0) {
            tooltip.add(Component.translatable("tooltip.magicadventurecore.gem_of_life.pour")
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("tooltip.magicadventurecore.gem_of_life.charge")
                    .withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    /**
     * Пока игрок держит ПКМ, каждый тик переливаем эссенцию в ближайший резервуар
     * и отправляем клиентам красные орбы, летящие от игрока к урне.
     */
    @Override
    public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remainingUseDuration) {
        if (!(living instanceof Player player)) {
            return;
        }
        // Логика и частицы — только на сервере; клиент получит орбы пакетом.
        if (level.isClientSide) {
            return;
        }
        int charge = getEssence(stack);
        if (charge <= 0) {
            player.stopUsingItem();
            return;
        }
        BlockPos target = findNearestReservoir(level, player.blockPosition());
        if (target == null || !(level.getBlockEntity(target) instanceof ERBlockEntity reservoir)) {
            player.stopUsingItem();
            return;
        }
        EssenceType current = reservoir.getEssenceType();
        int space = ERBlockEntity.MAX_ESSENCE - reservoir.getEssenceAmount();
        if (space <= 0 || (current != EssenceType.NONE && current != EssenceType.LIFE)) {
            player.stopUsingItem();
            return;
        }
        int accepted = reservoir.addEssence(EssenceType.LIFE, Math.min(POUR_PER_TICK, Math.min(charge, space)));
        if (accepted <= 0) {
            player.stopUsingItem();
            return;
        }
        setEssence(stack, charge - accepted);

        if (level instanceof ServerLevel serverLevel) {
            Vec3 from = new Vec3(player.getX(), player.getY() + 1.2D, player.getZ());
            Vec3 to = new Vec3(target.getX() + 0.5D, target.getY() + 0.7D, target.getZ() + 0.5D);
            serverLevel.sendParticles(new LifeOrbParticleData(to),
                    from.x, from.y, from.z, 2, 0.35D, 0.25D, 0.35D, 0.03D);
        }
    }

    @Nullable
    private static BlockPos findNearestReservoir(Level level, BlockPos center) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos from = center.offset(-POUR_RADIUS, -POUR_RADIUS, -POUR_RADIUS);
        BlockPos to = center.offset(POUR_RADIUS, POUR_RADIUS, POUR_RADIUS);
        for (BlockPos candidate : BlockPos.betweenClosed(from, to)) {
            if (!level.getBlockState(candidate).is(MACBlocks.ESSENCE_RESERVOIR.get())) {
                continue;
            }
            double distance = candidate.distSqr(center);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate.immutable();
            }
        }
        return best;
    }
}
