package latibro.minecraft.swap.swapper;

import latibro.minecraft.swap.SwapMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import static latibro.minecraft.swap.SwapMod.SWAPPER_BLOCK_ENTITY;

public class SwapperBlockEntity extends TileEntity {

    private BlockData storedTargetData;

    public SwapperBlockEntity() {
        super(SWAPPER_BLOCK_ENTITY.get());
    }

    public void swap() {
        SwapMod.LOGGER.warn("EEE swapping");

        BlockData currentTargetData = getWorldTargetData();

        // Set new to stored
        BlockData newBlockData = getStoredTargetData();

        // Set stored to current
        setStoredTargetData(currentTargetData);

        // Swap current for new
        setWorldTargetData(newBlockData);
    }

    private BlockPos getTargetPos() {
        return getBlockPos().above();
    }

    private BlockData getStoredTargetData() {
        return storedTargetData;
    }

    private void setStoredTargetData(BlockData targetData) {
        storedTargetData = targetData;
    }

    private BlockData getWorldTargetData() {
        if (getLevel() == null) {
            return null;
        }

        BlockState blockState = getLevel().getBlockState(getTargetPos());
        TileEntity blockEntity = getLevel().getBlockEntity(getTargetPos());
        CompoundNBT nbt = null;

        if (blockEntity != null) {
            nbt = blockEntity.serializeNBT();
        }

        return new BlockData(blockState, nbt);
    }

    private void setWorldTargetData(BlockData targetData) {
        if (getLevel() == null) {
            return;
        }

        BlockState blockState = targetData != null ? targetData.blockState : null;
        if (blockState == null) {
            blockState = Blocks.AIR.defaultBlockState();
        }

        // Clear inventory to prevent item drops when removed
        TileEntity currentBlockEntity = getLevel().getBlockEntity(getTargetPos());
        if (currentBlockEntity instanceof IInventory) {
            ((IInventory) currentBlockEntity).clearContent();
        }

        getLevel().setBlockAndUpdate(getTargetPos(), blockState);

        TileEntity blockEntity = getLevel().getBlockEntity(getTargetPos());
        if (targetData != null && targetData.nbt != null && blockEntity != null) {
            blockEntity.deserializeNBT(targetData.nbt);
        }

        // notify neighbors of target
        getLevel().updateNeighborsAt(getTargetPos(), blockState.getBlock());
    }


    @Override
    public void load(BlockState blockState, CompoundNBT nbt) {
        SwapMod.LOGGER.warn("EEE load");
        super.load(blockState, nbt);
        CompoundNBT targetDataNbt = nbt.getCompound("SwapperTargetData");
        BlockState targetBlockState = NBTUtil.readBlockState(targetDataNbt.getCompound("BlockState"));
        CompoundNBT targetNbt = targetDataNbt.getCompound("NBT");
        storedTargetData = new BlockData(targetBlockState, targetNbt);
        SwapMod.LOGGER.warn("EEE load done");
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        SwapMod.LOGGER.warn("EEE save");
        super.save(nbt);
        CompoundNBT targetDataNbt = new CompoundNBT();
        targetDataNbt.put("BlockState", NBTUtil.writeBlockState(storedTargetData.blockState));
        targetDataNbt.put("NBT", storedTargetData.nbt);
        nbt.put("SwapperTargetData", targetDataNbt);
        SwapMod.LOGGER.warn("EEE save done");
        return nbt;
    }

    private static class BlockData {
        final BlockState blockState;
        final CompoundNBT nbt;

        BlockData(BlockState blockState, CompoundNBT nbt) {
            //TODO assert that blockState is not null
            this.blockState = blockState;
            this.nbt = nbt;
        }
    }

}
