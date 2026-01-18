package com.ptdteam.magicadventurecore.world.block;

import com.ptdteam.magicadventurecore.registry.MACItems;
import com.ptdteam.magicadventurecore.world.block.entity.ERBlockEntity;
import com.ptdteam.magicadventurecore.world.essence.EssenceType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ERBlock extends Block implements EntityBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);
    private static final ResourceLocation BLOOD_FLUID_ID = ResourceLocation.fromNamespaceAndPath("bloodmagic", "life_essence_fluid");

    public ERBlock(Properties properties) {
        super(properties.mapColor(MapColor.METAL));
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (level.getBlockEntity(pos) instanceof ERBlockEntity blockEntity) {
            if (!level.isClientSide) {
                ItemStack held = player.getItemInHand(hand);
                if (tryFillWithBlood(blockEntity, player, hand, held)) {
                    return InteractionResult.CONSUME;
                }
                String essenceId = blockEntity.getEssenceType().getId();
                int amount = blockEntity.getEssenceAmount();
                player.displayClientMessage(Component.literal(essenceId + " = " + amount), false);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private boolean tryFillWithBlood(
            ERBlockEntity blockEntity,
            Player player,
            InteractionHand hand,
            ItemStack held
    ) {
        if (held.isEmpty()) {
            return false;
        }
        Fluid bloodFluid = ForgeRegistries.FLUIDS.getValue(BLOOD_FLUID_ID);
        if (bloodFluid == null) {
            return false;
        }
        int space = ERBlockEntity.MAX_ESSENCE - blockEntity.getEssenceAmount();
        if (space <= 0) {
            return false;
        }
        return held.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).map(handler -> {
            FluidStack drained = handler.drain(space, IFluidHandler.FluidAction.SIMULATE);
            if (drained.isEmpty() || !drained.getFluid().isSame(bloodFluid)) {
                return false;
            }
            int accepted = blockEntity.addEssence(EssenceType.BLOOD, drained.getAmount());
            if (accepted <= 0) {
                return false;
            }
            handler.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
            ItemStack container = handler.getContainer();
            player.setItemInHand(hand, container);
            return true;
        }).orElse(false);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        ItemStack stack = new ItemStack(MACItems.ESSENCE_RESERVOIR.get());
        BlockEntity blockEntity = builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof ERBlockEntity reservoir) {
            reservoir.saveToItem(stack);
        }
        return List.of(stack);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ERBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof ERBlockEntity reservoir) {
            reservoir.loadFromItem(stack);
        }
    }
}