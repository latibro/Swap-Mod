package latibro.minecraft.swap.swapper;

import latibro.minecraft.swap.SwapMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import javax.annotation.Nullable;
import java.util.Objects;

public class SwapperBlock extends Block {

    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    public SwapperBlock() {
        super(Properties.of(Material.METAL));

        this.registerDefaultState(this.defaultBlockState().setValue(TRIGGERED, false));
        //this.registerDefaultState(this.stateDefinition.any().setValue(STATE, 0));
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
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean x) {
        if (!world.isClientSide) {
            boolean previousPowered = state.getValue(TRIGGERED);
            boolean currentPowered = world.getDirectSignalTo(pos) > 0;
            //SwapMod.LOGGER.warn("SWAPPER " + previousPowered + " -> " + currentPowered + " " + pos + " " + fromPos + " " + state + " " + x);

            if (previousPowered == currentPowered) {
                // No powered change
                //SwapMod.LOGGER.warn("SWAPPER no change");
                return;
            }

            if (previousPowered) {
                // Power off
                //SwapMod.LOGGER.warn("SWAPPER power off");
                world.setBlockAndUpdate(pos, state.setValue(TRIGGERED, false));
                return;
            }

            //SwapMod.LOGGER.warn("SWAPPER power on");
            world.setBlockAndUpdate(pos, state.setValue(TRIGGERED, true));

            SwapMod.LOGGER.warn("SWAPPER swap");

            ((SwapperBlockEntity) world.getBlockEntity(pos)).swap();
        }
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> state) {
        state.add(TRIGGERED);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        //TODO set state based on neigbor redstone
        return super.getStateForPlacement(context);
    }
}
