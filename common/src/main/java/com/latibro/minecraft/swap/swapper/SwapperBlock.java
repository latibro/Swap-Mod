package com.latibro.minecraft.swap.swapper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SwapperBlock extends Block implements EntityBlock {
    //TODO explore if codec is needed? https://docs.minecraftforge.net/en/1.20.x/datastorage/codecs/

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    private static final Properties DEFAULT_PROPERTIES =
            BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).requiresCorrectToolForDrops().strength(3.5F);

    public SwapperBlock() {
        super(DEFAULT_PROPERTIES);

        //TODO this should use some kind of state builder, instead of chaining (BlockState.setValue name is a lie, it should be getStateWithValue, as it return a new state)
        var defaultBlockState = this.defaultBlockState();
        var defaultBlockStateWithFacing = defaultBlockState.setValue(FACING, Direction.NORTH);
        var defaultBlockStateWithTriggered = defaultBlockStateWithFacing.setValue(TRIGGERED, false);
        registerDefaultState(defaultBlockStateWithTriggered);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new SwapperBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        var defaultBlockState = defaultBlockState();
        var facingDirection = pContext.getNearestLookingDirection().getOpposite();
        var facingBlockState = defaultBlockState.setValue(FACING, facingDirection);
        return facingBlockState;
    }

    @Override
    protected void neighborChanged(@NotNull BlockState state,
                                   Level level,
                                   @NotNull BlockPos pos,
                                   @NotNull Block neighborBlock,
                                   @NotNull BlockPos neighborPos,
                                   boolean movedByPiston) {
        if (!level.isClientSide) {
            boolean previousPowered = state.getValue(TRIGGERED);
            boolean currentPowered = level.getDirectSignalTo(pos) > 0;

            if (previousPowered == currentPowered) {
                // No powered change
                return;
            }

            if (previousPowered) {
                // Power off
                level.setBlockAndUpdate(pos, state.setValue(TRIGGERED, false));
                return;
            }

            // Power on
            level.setBlockAndUpdate(pos, state.setValue(TRIGGERED, true));

            var blockEntity = (SwapperBlockEntity) level.getBlockEntity(pos);
            blockEntity.swap();
        }

        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    @Override
    protected @NotNull BlockState rotate(BlockState pState, Rotation pRotation) {
        var currentFacing = pState.getValue(FACING);
        var rotatedFacing = pRotation.rotate(currentFacing);
        //TODO should use some kind of state builder
        var rotatedState = pState.setValue(FACING, rotatedFacing);
        return rotatedState;
    }

    @Override
    protected @NotNull BlockState mirror(BlockState pState, Mirror pMirror) {
        var currentFacing = pState.getValue(FACING);
        var rotation = pMirror.getRotation(currentFacing);
        var mirroredFacing = rotation.rotate(currentFacing);
        //TODO should use some kind of state builder
        var mirroredState = pState.setValue(FACING, mirroredFacing);
        return mirroredState;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TRIGGERED);
        super.createBlockStateDefinition(builder);
    }

}
