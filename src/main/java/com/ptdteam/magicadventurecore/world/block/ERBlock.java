package com.ptdteam.magicadventurecore.world.block;

import com.ptdteam.magicadventurecore.world.block.entity.ERBlockEntity;
import com.ptdteam.magicadventurecore.world.essence.EssenceType;
import com.ptdteam.magicadventurecore.world.item.GemOfLifeItem;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class ERBlock extends Block implements EntityBlock {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);
    private static final ResourceLocation BLOOD_FLUID_ID = ResourceLocation.fromNamespaceAndPath("bloodmagic", "life_essence");
    private static final VoxelShape SHAPE = Block.box(3D, 0D, 3D, 13D, 10D, 13D);

    public ERBlock(Properties properties) {
        super(properties.mapColor(MapColor.METAL));
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return SHAPE;
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
        if (!(level.getBlockEntity(pos) instanceof ERBlockEntity blockEntity)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }
        ItemStack held = player.getItemInHand(hand);
        // Гем Жизни: начинаем «канал» переливания, дальше каждый тик работает
        // GemOfLifeItem#onUseTick (орбы -> резервуар).
        if (held.getItem() instanceof GemOfLifeItem && GemOfLifeItem.getEssence(held) > 0) {
            player.startUsingItem(hand);
            return InteractionResult.sidedSuccess(false);
        }
        if (tryFillWithBlood(blockEntity, player, hand, held)) {
            return InteractionResult.CONSUME;
        }
        player.displayClientMessage(
                Component.translatable("message.magicadventurecore.reservoir_status",
                        blockEntity.getEssenceType().getDisplayName(),
                        blockEntity.getEssenceAmount()),
                false
        );
        return InteractionResult.sidedSuccess(false);
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
            // Заменяем ровно один предмет из стака: раньше весь стак подменялся одним
            // контейнером, и лишние вёдра/капсулы пропадали.
            held.shrink(1);
            if (held.isEmpty()) {
                player.setItemInHand(hand, container);
            } else if (!player.getInventory().add(container)) {
                player.drop(container, false);
            }
            return true;
        }).orElse(false);
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