package com.ptdteam.magicadventurecore.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Метка мела на земле: тонкий «рисунок» 1/16 блока, без коллизии и без дропа.
 * Ставится и стирается магическим мелом; живёт только на твёрдой опоре.
 *
 * <p>Когда круг замкнут, контроллер переводит все метки в {@link #DRAWN} = true:
 * блок остаётся (участвует в проверке круга), но модель не рисуется — вместо мелких
 * символов появляется цельный рисунок круга.</p>
 */
public class ChalkMarkBlock extends Block {
    public static final BooleanProperty DRAWN = BooleanProperty.create("drawn");

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 0.25D, 16.0D);

    public ChalkMarkBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DRAWN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DRAWN);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Контур остаётся даже у «проявленной» метки — чтобы круг можно было разобрать.
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    public boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (!canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
