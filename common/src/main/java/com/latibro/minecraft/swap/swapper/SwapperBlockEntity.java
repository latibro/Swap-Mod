package com.latibro.minecraft.swap.swapper;

import com.latibro.minecraft.swap.Constants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

import static com.latibro.minecraft.swap.CommonClass.SWAPPER_BLOCK_ENTITY_TYPE;

public class SwapperBlockEntity extends BlockEntity {

    public static final Block[] BLACKLISTED_BLOCKS = {
            Blocks.BEDROCK, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK
    };

    private BlockPos targetPos;
    private StoredBlockData storedTargetData;

    public SwapperBlockEntity(BlockPos pos, BlockState state) {
        super(SWAPPER_BLOCK_ENTITY_TYPE, pos, state);
    }

    public void swap() {
        Constants.LOG.warn("SWAPPER swapping");

        // Check if target position is the same as swapper position
        if (getBlockPos().equals(getTargetPos())) {
            Constants.LOG.warn("SWAPPER target and swapper is at same position");
            return;
        }

        StoredBlockData blockToBeStored = getPlacedBlock();

        // Check if target is blacklisted
        if (Arrays.stream(BLACKLISTED_BLOCKS).anyMatch(blockToBeStored.blockState.getBlock()::equals)) {
            Constants.LOG.warn("SWAPPER target is blacklisted");
            return;
        }

        // Check if target position is loaded (chunk is loaded)
        if (!getLevel().isLoaded(getTargetPos())) {
            Constants.LOG.warn("SWAPPER target is not loaded");
            return;
        }

        // Set new to stored
        StoredBlockData blockToBePlaced = getStoredBlock();

        // Set stored to current
        storeBlock(blockToBeStored);

        // Swap current for new
        placeBlock(blockToBePlaced);

        // Notify clients about the change
        // Same flags Level.setBlock() - 1+2=3. 1 will notify neighboring blocks through neighborChanged updates. 2 will send the change to clients.
        getLevel().sendBlockUpdated(getTargetPos(), blockToBePlaced.blockState, storedTargetData.blockState, 3);
    }

    private BlockPos getTargetPos() {
        if (targetPos == null) {
            targetPos = getBlockPos().above();
        }
        return targetPos;
    }

    private StoredBlockData getStoredBlock() {
        if (storedTargetData == null) {
            Constants.LOG.warn("SWAPPER stored target data is null");
            storedTargetData = new StoredBlockData(Blocks.BLUE_WOOL.defaultBlockState(), null);
        }
        return storedTargetData;
    }

    private void storeBlock(StoredBlockData targetData) {
        storedTargetData = targetData;
    }

    public boolean hasStoredTargetData() {
        return storedTargetData != null && !storedTargetData.blockState.is(Blocks.AIR);
    }

    private StoredBlockData getPlacedBlock() {
        BlockState targetBlockState = getLevel().getBlockState(getTargetPos());
        BlockEntity targetBlockEntity = getLevel().getBlockEntity(getTargetPos());

        if (targetBlockEntity == null) {
            return new StoredBlockData(targetBlockState, null);
        }

        var customData = targetBlockEntity.components().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var targetBlockTag = customData.copyTag();

        return new StoredBlockData(targetBlockState, targetBlockTag);
    }

    private void placeBlock(StoredBlockData blockData) {
        BlockState blockState = blockData == null
                                ? Blocks.GREEN_WOOL.defaultBlockState()
                                : blockData.blockState;

        // Clear inventory to prevent item drops when removed (replaced)
        BlockEntity currentlyPlacedBlockEntity = getLevel().getBlockEntity(getTargetPos());
        if (currentlyPlacedBlockEntity instanceof Container container) {
            Constants.LOG.debug("SWAPPER clearing container of current block before replace");
            container.clearContent();
        }

        // Place new block (replace the old block)
        Constants.LOG.debug("SWAPPER placing stored block");
        getLevel().setBlockAndUpdate(getTargetPos(), blockState);

        // If any tag was stored, then we
        if (blockData.blockEntityTag != null) {
            Constants.LOG.debug("SWAPPER has stored tag");

            BlockEntity blockEntity = getLevel().getBlockEntity(getTargetPos());

            if (blockEntity == null) {
                Constants.LOG.debug("SWAPPER has stored tag but did not find block entity");
            } else {
                Constants.LOG.debug("SWAPPER updating tag on block entity");

                var customData = blockEntity.components().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                var customDataTag = customData.copyTag();

                DataComponentPatch dataComponentPatch
                        = DataComponentPatch.builder().set(DataComponents.CUSTOM_DATA, CustomData.of(customDataTag)).build();

                blockEntity.applyComponents(blockEntity.components(), dataComponentPatch);
            }
        }

        // notify target self - to make the newly placed block react to neighbor states
        getLevel().neighborChanged(getTargetPos(), blockState.getBlock(), getTargetPos());

        // notify neighbors of target - to make neighbors react to newly placed block
        getLevel().updateNeighborsAt(getTargetPos(), blockState.getBlock());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        this.targetPos = NbtUtils.readBlockPos(tag, "SwapperTargetPos").get();

        CompoundTag targetDataNbt = tag.getCompound("SwapperTargetData");
        CompoundTag targetBlockStateNbt = targetDataNbt.getCompound("BlockState");
        BlockState targetBlockState
                = NbtUtils.readBlockState(this.getLevel().holderLookup(Registries.BLOCK), targetBlockStateNbt);
        CompoundTag targetNbt = targetDataNbt.getCompound("Tag");
        storeBlock(new StoredBlockData(targetBlockState, targetNbt));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put("SwapperTargetPos", NbtUtils.writeBlockPos(getTargetPos()));

        CompoundTag targetDataNbt = new CompoundTag();
        var targetBlockStateNbt = NbtUtils.writeBlockState(getStoredBlock().blockState);
        targetDataNbt.put("BlockState", targetBlockStateNbt);
        if (getStoredBlock().blockEntityTag != null) {
            targetDataNbt.put("Tag", getStoredBlock().blockEntityTag);
        }
        tag.put("SwapperTargetData", targetDataNbt);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static class StoredBlockData {
        final BlockState blockState;
        final CompoundTag blockEntityTag;

        StoredBlockData(BlockState blockState, CompoundTag blockEntityTag) {
            this.blockState = blockState;
            this.blockEntityTag = blockEntityTag;
        }
    }

}
