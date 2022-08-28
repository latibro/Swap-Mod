package latibro.minecraft.swap.swapper;

import latibro.minecraft.swap.SwapMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

import static latibro.minecraft.swap.SwapMod.SWAPPER_BLOCK_ENTITY;

public class SwapperBlockEntity extends TileEntity {

    private static final Block[] BLACKLISTED_BLOCKS = {
            Blocks.BEDROCK,
            Blocks.COMMAND_BLOCK,
            Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK
    };

    private BlockPos targetPos;
    private BlockData storedTargetData;

    public SwapperBlockEntity() {
        super(SWAPPER_BLOCK_ENTITY.get());
    }

    public void swap() {
        SwapMod.LOGGER.warn("SWAPPER swapping");

        // Check if target position is the same as swapper position
        if (getBlockPos().equals(getTargetPos())) {
            SwapMod.LOGGER.warn("SWAPPER target and swapper is at same position");
            return;
        }

        BlockData currentTargetData = getWorldTargetData();

        // Check if target is blacklisted
        if (Arrays.stream(BLACKLISTED_BLOCKS).anyMatch(currentTargetData.blockState.getBlock()::equals)) {
            SwapMod.LOGGER.warn("SWAPPER target is blacklisted");
            return;
        }

        // Set new to stored
        BlockData newTargetData = getStoredTargetData();

        // Set stored to current
        setStoredTargetData(currentTargetData);

        // Swap current for new
        setWorldTargetData(newTargetData);
    }

    private BlockPos getTargetPos() {
        if (targetPos == null) {
            targetPos = getBlockPos().above();
        }
        return targetPos;
    }

    private BlockData getStoredTargetData() {
        if (storedTargetData == null) {
            storedTargetData = new BlockData(Blocks.AIR.defaultBlockState(), null);
        }
        return storedTargetData;
    }

    private void setStoredTargetData(BlockData targetData) {
        storedTargetData = targetData;
    }

    private BlockData getWorldTargetData() {
        if (getLevel() == null) {
            return null;
        }

        BlockState targetBlockState = getLevel().getBlockState(getTargetPos());
        TileEntity targetBlockEntity = getLevel().getBlockEntity(getTargetPos());
        CompoundNBT targetNbt = null;

        if (targetBlockEntity != null) {
            targetNbt = targetBlockEntity.serializeNBT();
        }

        return new BlockData(targetBlockState, targetNbt);
    }

    private void setWorldTargetData(BlockData targetData) {
        if (getLevel() == null) {
            return;
        }

        BlockState targetBlockState = targetData != null ? targetData.blockState : null;
        if (targetBlockState == null) {
            targetBlockState = Blocks.AIR.defaultBlockState();
        }

        // Clear inventory to prevent item drops when removed
        TileEntity currentTargetBlockEntity = getLevel().getBlockEntity(getTargetPos());
        if (currentTargetBlockEntity instanceof IInventory) {
            ((IInventory) currentTargetBlockEntity).clearContent();
        }

        getLevel().setBlockAndUpdate(getTargetPos(), targetBlockState);

        TileEntity targetBlockEntity = getLevel().getBlockEntity(getTargetPos());
        if (targetData != null && targetData.nbt != null && targetBlockEntity != null) {
            targetBlockEntity.deserializeNBT(targetData.nbt);
        }

        // notify neighbors of target
        getLevel().updateNeighborsAt(getTargetPos(), targetBlockState.getBlock());
    }


    @Override
    public void load(BlockState blockState, CompoundNBT nbt) {
        super.load(blockState, nbt);

        targetPos = NBTUtil.readBlockPos(nbt.getCompound("SwapperTargetPos"));

        CompoundNBT targetDataNbt = nbt.getCompound("SwapperTargetData");
        BlockState targetBlockState = NBTUtil.readBlockState(targetDataNbt.getCompound("BlockState"));
        CompoundNBT targetNbt = targetDataNbt.getCompound("NBT");
        setStoredTargetData(new BlockData(targetBlockState, targetNbt));
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);

        nbt.put("SwapperTargetPos", NBTUtil.writeBlockPos(getTargetPos()));

        CompoundNBT targetDataNbt = new CompoundNBT();
        targetDataNbt.put("BlockState", NBTUtil.writeBlockState(getStoredTargetData().blockState));
        if (getStoredTargetData().nbt != null) {
            targetDataNbt.put("NBT", getStoredTargetData().nbt);
        }
        nbt.put("SwapperTargetData", targetDataNbt);
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
