package com.latibro.minecraft.swap.swapper;

import com.latibro.minecraft.swap.Constants;
import com.latibro.minecraft.swap.platform.Services;
import com.latibro.minecraft.swap.platform.services.RegistryService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
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

public class SwapperBlockEntity extends BlockEntity {

    public static final Block[] BLACKLISTED_BLOCKS = {
            Blocks.BEDROCK, Blocks.COMMAND_BLOCK, Blocks.CHAIN_COMMAND_BLOCK, Blocks.REPEATING_COMMAND_BLOCK
    };

    private BlockPos targetBlockPos;

    private StoredBlockData storedBlockData;

    public SwapperBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(Services.get(RegistryService.class).getBlockEntityType("swapper"), blockPos, blockState);
    }

    public void swap() {
        Constants.LOG.info("SWAPPER swapping");

        // Check if target position is the same as swapper position
        if (getBlockPos().equals(getTargetBlockPos())) {
            Constants.LOG.warn("SWAPPER target and swapper is at same position");
            return;
        }

        StoredBlockData blockDataToBeStored = getBlockDataOfBlockCurrentlyPlacedAtTargetBlockPos();
        Constants.LOG.info("SWAPPER block to be stored {}", blockDataToBeStored);

        // Check if target is blacklisted
        var isBlockBlacklisted
                = Arrays.stream(BLACKLISTED_BLOCKS).anyMatch(blockDataToBeStored.blockState.getBlock()::equals);
        if (isBlockBlacklisted) {
            Constants.LOG.info("SWAPPER target is blacklisted");
            return;
        }

        // Check if target position is loaded (chunk is loaded)
        var isTargetBlockPosLoaded = getLevel().isLoaded(getTargetBlockPos());
        if (!isTargetBlockPosLoaded) {
            Constants.LOG.info("SWAPPER target is not loaded");
            return;
        }

        // Set new to stored
        StoredBlockData blockDataToBePlaced = getStoredBlockData();
        Constants.LOG.info("SWAPPER block to be placed {}", blockDataToBePlaced);

        // Set stored to current
        storeBlockData(blockDataToBeStored);

        // Swap current for new
        placeBlock(blockDataToBePlaced);

        // Notify clients about the change
        // Same flags Level.setBlock() - 1+2=3. 1 will notify neighboring blocks through neighborChanged updates. 2 will send the change to clients.
        getLevel().sendBlockUpdated(getTargetBlockPos(), blockDataToBePlaced.blockState, storedBlockData.blockState, 3);
    }

    private BlockPos getTargetBlockPos() {
        if (this.targetBlockPos == null) {
            this.targetBlockPos = getBlockPos().above();
        }
        return this.targetBlockPos;
    }

    private void setTargetBlockPos(BlockPos blockPos) {
        if (blockPos == null) {
            this.targetBlockPos = getBlockPos().above();
        } else {
            this.targetBlockPos = blockPos;
        }
    }

    private StoredBlockData getStoredBlockData() {
        if (storedBlockData == null) {
            Constants.LOG.info("SWAPPER stored target data is null");
            storedBlockData = new StoredBlockData(Blocks.AIR.defaultBlockState(), null);
        }
        return storedBlockData;
    }

    private void storeBlockData(StoredBlockData targetData) {
        storedBlockData = targetData;
    }

    public boolean hasStoredTargetData() {
        return storedBlockData != null && !storedBlockData.blockState.is(Blocks.AIR);
    }

    private StoredBlockData getBlockDataOfBlockCurrentlyPlacedAtTargetBlockPos() {
        BlockState targetBlockState = getLevel().getBlockState(getTargetBlockPos());
        BlockEntity targetBlockEntity = getLevel().getBlockEntity(getTargetBlockPos());

        if (targetBlockEntity == null) {
            return new StoredBlockData(targetBlockState, null);
        }

        var customData = targetBlockEntity.components().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var targetBlockTag = customData.copyTag();

        return new StoredBlockData(targetBlockState, targetBlockTag);
    }

    private void placeBlock(StoredBlockData blockData) {
        BlockState blockState = blockData.blockState;

        if (blockState == null) {
            Constants.LOG.info("SWAPPER no block state - defaulting");
            blockState = Blocks.AIR.defaultBlockState();
        }

        // Clear inventory to prevent item drops when removed (replaced)
        BlockEntity currentlyPlacedBlockEntity = getLevel().getBlockEntity(getTargetBlockPos());
        if (currentlyPlacedBlockEntity instanceof Container container) {
            Constants.LOG.info("SWAPPER clearing container of current block before replace");
            container.clearContent();
        }

        // Place new block (replace the old block)
        Constants.LOG.info("SWAPPER placing stored block");
        getLevel().setBlockAndUpdate(getTargetBlockPos(), blockState);

        // If any tag was stored, then we
        if (blockData.blockEntityTag != null) {
            Constants.LOG.info("SWAPPER has stored tag {}", blockData.blockEntityTag);

            BlockEntity blockEntity = getLevel().getBlockEntity(getTargetBlockPos());

            if (blockEntity == null) {
                Constants.LOG.info("SWAPPER has stored tag but did not find block entity");
            } else {
                Constants.LOG.info("SWAPPER updating tag on block entity");

                var customData = blockEntity.components().getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                var customDataTag = customData.copyTag();

                DataComponentPatch dataComponentPatch
                        = DataComponentPatch.builder()
                                            .set(DataComponents.CUSTOM_DATA, CustomData.of(customDataTag))
                                            .build();

                blockEntity.applyComponents(blockEntity.components(), dataComponentPatch);
            }
        }

        // notify target self - to make the newly placed block react to neighbor states
        getLevel().neighborChanged(getTargetBlockPos(), blockState.getBlock(), getTargetBlockPos());

        // notify neighbors of target - to make neighbors react to newly placed block
        getLevel().updateNeighborsAt(getTargetBlockPos(), blockState.getBlock());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        Constants.LOG.info("SWAPPER loading data {}", tag);

        super.loadAdditional(tag, registries);

        var storedTargetBlockPos = NbtUtils.readBlockPos(tag, "SwapperTargetPos").get();
        setTargetBlockPos(storedTargetBlockPos);

        var storedBlockDataTag = tag.getCompound("SwapperTargetData");

        var storedBlockStateTag = storedBlockDataTag.getCompound("BlockState");
        HolderGetter<Block> holderGetter = (HolderGetter<Block>) (this.level != null
                                                                  ? this.level.holderLookup(Registries.BLOCK)
                                                                  : BuiltInRegistries.BLOCK.asLookup());
        var storedBlockState
                = NbtUtils.readBlockState(holderGetter, storedBlockStateTag);

        var storedBlockEntityTag = storedBlockDataTag.getCompound("Tag");

        storeBlockData(new StoredBlockData(storedBlockState, storedBlockEntityTag));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        var targetBlockPos = getTargetBlockPos();
        var targetBlockPosTag = NbtUtils.writeBlockPos(targetBlockPos);
        tag.put("SwapperTargetPos", targetBlockPosTag);

        var storedBlockData = getStoredBlockData();
        CompoundTag storedBlockDataTag = new CompoundTag();

        var storedBlockState = storedBlockData.blockState;
        var storedBlockStateTag = NbtUtils.writeBlockState(storedBlockState);
        storedBlockDataTag.put("BlockState", storedBlockStateTag);

        var storedBlockEntityTag = storedBlockData.blockEntityTag;
        if (storedBlockEntityTag != null) {
            storedBlockDataTag.put("Tag", storedBlockEntityTag);
        }
        tag.put("SwapperTargetData", storedBlockDataTag);

        Constants.LOG.info("SWAPPER saved data {}", tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        Constants.LOG.info("SWAPPER getUpdatePacket");
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        Constants.LOG.info("SWAPPER getUpdateTag");
        //TODO do client need to know any details?
        return this.saveCustomOnly(registries);
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
