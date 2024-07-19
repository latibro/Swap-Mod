package com.latibro.minecraft.swap.swapper;

import com.latibro.minecraft.swap.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

public class SwapperBlock extends Block implements EntityBlock {

    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    private static final Properties DEFAULT_PROPERTIES = BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).requiresCorrectToolForDrops().strength(3.5F);

    public SwapperBlock() {
        super(DEFAULT_PROPERTIES);
        registerDefaultState(defaultBlockState().setValue(TRIGGERED, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SwapperBlockEntity(pos, state);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        Constants.LOG.debug("neighborChanged : {}", pos);

        if (!level.isClientSide) {
            boolean previousPowered = state.getValue(TRIGGERED);
            boolean currentPowered = level.getDirectSignalTo(pos) > 0;

            if (previousPowered==currentPowered) {
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

            ((SwapperBlockEntity) level.getBlockEntity(pos)).swap();
        }

        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (((SwapperBlockEntity) blockEntity).hasStoredTargetData()) {
            Constants.LOG.warn("SWAPPER remove - holds stored data");
            player.displayClientMessage(Component.literal("Swapper has stored block"), false);
            return;
        }

        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRIGGERED);
        super.createBlockStateDefinition(builder);
    }

}
