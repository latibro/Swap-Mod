package latibro.minecraft.swap.swapper;

import latibro.minecraft.swap.SwapMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;

import static latibro.minecraft.swap.SwapMod.SWAPPER_BLOCK_ENTITY;

public class SwapperBlockEntity extends TileEntity {

    //TODO store storedBlockState in NBT
    private BlockState storedBlockState = Blocks.AIR.defaultBlockState();
    //TODO store storedNBT in NBT
    private CompoundNBT storedNBT = null;

    public SwapperBlockEntity() {
        super(SWAPPER_BLOCK_ENTITY.get());
    }

    public void swap() {
        SwapMod.LOGGER.warn("EEE swapping");

        SwapMod.LOGGER.warn("EEE stored state " + storedBlockState);
        if (storedNBT != null) {
            SwapMod.LOGGER.warn("EEE stored NBT " + storedNBT);
        }

        BlockState currentBlockState = getLevel().getBlockState(getBlockPos().above());
        TileEntity currentBlockEntity = getLevel().getBlockEntity(getBlockPos().above());
        CompoundNBT currentNBT = null;

        SwapMod.LOGGER.warn("EEE current state " + currentBlockState);
        SwapMod.LOGGER.warn("EEE current entity " + currentBlockEntity);
        if (currentBlockEntity != null) {
            currentNBT = currentBlockEntity.serializeNBT();
            SwapMod.LOGGER.warn("EEE current NBT " + currentNBT);
        }

        // Set new to stored
        BlockState newBlockState = storedBlockState;
        CompoundNBT newNBT = storedNBT;

        // Set stored to current
        storedBlockState = currentBlockState;
        storedNBT = currentNBT;

        // Clear inventory to prevent item drops when removed
        if (currentBlockEntity instanceof IInventory) {
            ((IInventory) currentBlockEntity).clearContent();
        }

        // Swap current for new
        getLevel().setBlockAndUpdate(getBlockPos().above(), newBlockState);
        SwapMod.LOGGER.warn("EEE new state " + newBlockState);
        TileEntity newBlockEntity = getLevel().getBlockEntity(getBlockPos().above());
        SwapMod.LOGGER.warn("EEE new entity " + newBlockEntity);
        if (newBlockEntity != null) {
            newBlockEntity.deserializeNBT(newNBT);
            SwapMod.LOGGER.warn("EEE new NBT " + newNBT);
        }

        // notify neighbors
        getLevel().updateNeighborsAt(getBlockPos().above(), getBlockState().getBlock());
    }

}
