package latibro.minecraft.swap.swapper;

import latibro.minecraft.swap.SwapMod;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

import static latibro.minecraft.swap.SwapMod.SWAPPER_BLOCK_ENTITY;

public class SwapperBlockEntity extends TileEntity {

    private BlockPos targetPos;
    private BlockData storedTargetData;

    public SwapperBlockEntity() {
        super(SWAPPER_BLOCK_ENTITY.get());
    }

    public void swap() {
        SwapMod.LOGGER.warn("SWAPPER swapping " + getTargetPos());

        BlockData currentTargetData = getWorldTargetData();

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

    public void highlightTarget() {
        SwapMod.LOGGER.warn("Particles " + getTargetPos());
        if (getLevel().isClientSide) {
            getLevel().addParticle(ParticleTypes.POOF, getTargetPos().getX() + 0.5, getTargetPos().getY() + 0.5, getTargetPos().getZ() + 0.5, 0.0, 0.0, 0);
        }
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

    @Override
    public CompoundNBT getUpdateTag() {
        //TODO maybe only target position
        return save(new CompoundNBT());
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) {
        //TODO maybe only target position
        load(state, tag);
    }

    @Nullable
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        return new SUpdateTileEntityPacket(getBlockPos(), 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet) {
        handleUpdateTag(getBlockState(), packet.getTag());
    }
}
