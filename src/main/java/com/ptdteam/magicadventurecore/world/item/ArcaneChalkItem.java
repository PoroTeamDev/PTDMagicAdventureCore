package com.ptdteam.magicadventurecore.world.item;

import com.ptdteam.magicadventurecore.registry.MACBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Магический мел. ПКМ по твёрдому блоку ставит метку мела на землю, зажатая ПКМ —
 * «рисует» подряд, Shift+ПКМ по метке — стирает. Прочность 100: полного круга
 * (88 клеток) хватает с запасом.
 */
public class ArcaneChalkItem extends Item {
    public ArcaneChalkItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clicked = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clicked);

        // Shift+ПКМ по метке — стереть.
        if (clickedState.is(MACBlocks.CHALK_MARK.get())) {
            if (player != null && player.isShiftKeyDown()) {
                if (!level.isClientSide) {
                    level.removeBlock(clicked, false);
                    level.playSound(null, clicked, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.4F, 1.7F);
                }
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.FAIL;
        }

        BlockPos markPos = clicked.above();
        BlockState current = level.getBlockState(markPos);
        if (current.is(MACBlocks.CHALK_MARK.get())) {
            return InteractionResult.FAIL;
        }
        if (!current.canBeReplaced()) {
            return InteractionResult.FAIL;
        }
        // Метка ставится только на твёрдый верх.
        if (!clickedState.isFaceSturdy(level, clicked, Direction.UP)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            level.setBlock(markPos, MACBlocks.CHALK_MARK.get().defaultBlockState(), 3);
            level.playSound(null, markPos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.35F, 1.8F);
            ItemStack stack = context.getItemInHand();
            if (player != null && !player.getAbilities().instabuild) {
                int damage = stack.getDamageValue() + 1;
                if (damage >= stack.getMaxDamage()) {
                    stack.shrink(1);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F);
                } else {
                    stack.setDamageValue(damage);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.magicadventurecore.arcane_chalk"));
    }
}
