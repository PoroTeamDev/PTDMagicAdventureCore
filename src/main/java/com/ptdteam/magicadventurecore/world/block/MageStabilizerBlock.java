package com.ptdteam.magicadventurecore.world.block;

import com.ptdteam.magicadventurecore.registry.MACBlockEntities;
import com.ptdteam.magicadventurecore.world.block.entity.MageStabilizerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Магический стабилизатор: группа кристаллов на каменном основании.
 * Модель — обычный Blockbench-экспорт (models/block/stabilizer.json), поэтому
 * форма блока не куб: коллизия только по основанию, контур — с учётом кристаллов
 * (чтобы по ним можно было кликать).
 */
public class MageStabilizerBlock extends Block implements EntityBlock {
    /** Основание: 10×3×10 пикселей по центру блока. */
    private static final VoxelShape COLLISION = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 3.0D, 13.0D);
    /** Контур для клика/подсветки: основание + зона кристаллов. */
    private static final VoxelShape OUTLINE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 16.0D, 13.0D);

    public MageStabilizerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return OUTLINE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return COLLISION;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MageStabilizerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == MACBlockEntities.MAGE_STABILIZER.get()
                ? (tickerLevel, pos, tickerState, blockEntity) ->
                MageStabilizerBlockEntity.tick(tickerLevel, pos, tickerState,
                        (MageStabilizerBlockEntity) blockEntity)
                : null;
    }
}
