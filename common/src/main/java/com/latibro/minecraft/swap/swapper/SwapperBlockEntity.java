package com.latibro.minecraft.swap.swapper;

import com.latibro.minecraft.swap.Constants;
import com.latibro.minecraft.swap.platform.Services;
import com.latibro.minecraft.swap.platform.services.RegistryService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static com.latibro.minecraft.swap.swapper.SwapperBlock.FACING;

public class SwapperBlockEntity extends BlockEntity {

    public static final List<Block> BLACKLISTED_BLOCKS = List.of(
            Blocks.BEDROCK,
            Blocks.COMMAND_BLOCK,
            Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK
    );

    private static BlockEntityType<SwapperBlockEntity> getBlockEntityType() {
        var registryService = Services.get(RegistryService.class);
        var blockEntityType = registryService.getBlockEntityType("swapper");
        return (BlockEntityType<SwapperBlockEntity>) blockEntityType;
    }

    public SwapperBlockEntity(BlockPos blockPos, BlockState blockState) {
        super(getBlockEntityType(), blockPos, blockState);
    }

    private Direction getFacingDirection() {
        var blockState = getBlockState();
        var direction = blockState.getValue(FACING);
        return direction;
    }

    private BlockPos getFirstTargetBlockPos() {
        var blockPos = getBlockPos();
        var direction = getFacingDirection();
        var targetBlockPos = blockPos.relative(direction);
        return targetBlockPos;
    }

    private BlockPos getSecondTargetBlockPos() {
        var blockPos = getBlockPos();
        var direction = getFacingDirection();
        var targetBlockPos = blockPos.relative(direction.getOpposite());
        return targetBlockPos;
    }

    private List<BlockPos> getTargetBlocks() {
        return List.of(getFirstTargetBlockPos(), getSecondTargetBlockPos());
    }

    private boolean canSwap() {
        var level = getLevel();

        var allTargetsCanSwap = getTargetBlocks().stream().allMatch(blockPos -> {
            var isLoaded = level.isLoaded(blockPos);

            var blockState = level.getBlockState(blockPos);
            var block = blockState.getBlock();

            var isBlacklisted = BLACKLISTED_BLOCKS.contains(block);

            var targetCanSwap = isLoaded && !isBlacklisted;

            return targetCanSwap;
        });

        return allTargetsCanSwap;
    }

    public void swap() {
        Constants.LOG.info("SWAPPER swapping");

        if (!canSwap()) {
            Constants.LOG.info("SWAPPER unable to swap");
            return;
        }

        var firstTargetBlockPos = getFirstTargetBlockPos();
        var firstTargetBlockData = getBlockDataOfBlockCurrentlyPlacedAtTargetBlockPos(firstTargetBlockPos);

        var secondTargetBlockPos = getSecondTargetBlockPos();
        var secondTargetBlockData = getBlockDataOfBlockCurrentlyPlacedAtTargetBlockPos(secondTargetBlockPos);

        placeBlock(secondTargetBlockData, firstTargetBlockPos);
        placeBlock(firstTargetBlockData, secondTargetBlockPos);
    }

    private StoredBlockData getBlockDataOfBlockCurrentlyPlacedAtTargetBlockPos(BlockPos targetBlockPos) {
        var level = getLevel();
        BlockState targetBlockState = level.getBlockState(targetBlockPos);
        BlockEntity targetBlockEntity = level.getBlockEntity(targetBlockPos);

        if (targetBlockEntity == null) {
            var storedBlockDataWithoutBlockEntity = new StoredBlockData(targetBlockState, null);
            return storedBlockDataWithoutBlockEntity;
        }

        var targetComponents = targetBlockEntity.components();
        var targetCustomData = targetComponents.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var targetBlockTag = targetCustomData.copyTag();

        var storedBlockDataWithBlockEntity = new StoredBlockData(targetBlockState, targetBlockTag);
        return storedBlockDataWithBlockEntity;
    }

    private void placeBlock(StoredBlockData blockData, BlockPos targetBlockPos) {
        var level = getLevel();

        var oldBlockState = level.getBlockState(targetBlockPos);
        var newBlockState = blockData.blockState;

        BlockEntity currentlyPlacedBlockEntity = level.getBlockEntity(targetBlockPos);
        var oldBlockHasContainerThatNeedsToBeCleared = currentlyPlacedBlockEntity instanceof Container;
        if (oldBlockHasContainerThatNeedsToBeCleared) {
            // Clear inventory to prevent item drops when removed (replaced)
            Constants.LOG.info("SWAPPER clearing container of current block before replace");
            var container = (Container) currentlyPlacedBlockEntity;
            container.clearContent();
        }

        Constants.LOG.info("SWAPPER placing stored block");
        // Place new block (replace the old block)
        level.setBlockAndUpdate(targetBlockPos, newBlockState);

        // If any tag was stored, then we
        var newBlockHasStoredTag = (blockData.blockEntityTag != null);
        if (newBlockHasStoredTag) {
            Constants.LOG.info("SWAPPER has stored tag {}", blockData.blockEntityTag);

            var newBlockEntity = level.getBlockEntity(targetBlockPos);
            var newBlockHasBlockEntity = (newBlockEntity != null);

            if (!newBlockHasBlockEntity) {
                Constants.LOG.warn("SWAPPER has stored tag but did not find block entity");
            } else {
                Constants.LOG.info("SWAPPER updating tag on block entity");

                var newBlockComponents = newBlockEntity.components();

                var newCustomData = newBlockComponents.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                var newCustomDataTag = newCustomData.copyTag();

                var dataComponentPatch
                        = DataComponentPatch.builder()
                                            .set(DataComponents.CUSTOM_DATA, CustomData.of(newCustomDataTag))
                                            .build();

                newBlockEntity.applyComponents(newBlockComponents, dataComponentPatch);
            }
        }

        // notify target self - to make the newly placed block react to neighbor states
        level.neighborChanged(targetBlockPos, newBlockState.getBlock(), targetBlockPos);

        // notify neighbors of target - to make neighbors react to newly placed block
        level.updateNeighborsAt(targetBlockPos, newBlockState.getBlock());

        // Notify clients about the change
        // Same flags Level.setBlock() - 1+2=3. 1 will notify neighboring blocks through neighborChanged updates. 2 will send the change to clients.
        level.sendBlockUpdated(targetBlockPos, newBlockState, oldBlockState, 3);
    }

    private record StoredBlockData(BlockState blockState, CompoundTag blockEntityTag) {
    }

}
