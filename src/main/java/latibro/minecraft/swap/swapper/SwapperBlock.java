package latibro.minecraft.swap.swapper;

import latibro.minecraft.swap.SwapMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class SwapperBlock extends Block {

    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    public SwapperBlock() {
        super(Properties.of(Material.METAL));
        this.registerDefaultState(this.defaultBlockState().setValue(TRIGGERED, false));
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new SwapperBlockEntity();
    }

    @Override
    public ActionResultType use(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult rayTrace) {
        SwapMod.LOGGER.warn("XX");
        ((SwapperBlockEntity) world.getBlockEntity(pos)).highlightTarget();
        return ActionResultType.SUCCESS;
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean x) {
        if (!world.isClientSide) {
            boolean previousPowered = state.getValue(TRIGGERED);
            boolean currentPowered = world.getDirectSignalTo(pos) > 0;

            if (previousPowered == currentPowered) {
                // No powered change
                return;
            }

            if (previousPowered) {
                // Power off
                world.setBlockAndUpdate(pos, state.setValue(TRIGGERED, false));
                return;
            }

            // Power on
            world.setBlockAndUpdate(pos, state.setValue(TRIGGERED, true));

            ((SwapperBlockEntity) world.getBlockEntity(pos)).swap();
        }
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> state) {
        state.add(TRIGGERED);
    }
}
