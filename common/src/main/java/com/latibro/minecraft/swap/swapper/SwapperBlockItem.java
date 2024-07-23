package com.latibro.minecraft.swap.swapper;

import com.latibro.minecraft.swap.Constants;
import com.latibro.minecraft.swap.platform.Services;
import com.latibro.minecraft.swap.platform.services.RegistryService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

public class SwapperBlockItem extends BlockItem {

    public SwapperBlockItem(SwapperBlock block) {
        super(block, new Item.Properties());
    }

    //TODO inspiration https://fabricmc.net/2024/04/19/1205.html#:~:text=called%20%E2%80%9Creloadable%20registries%E2%80%9D.-,Item%20Components,-We%20skip%20the
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Constants.LOG.info("SWAPPER useOn");

        if (context.getPlayer().isCrouching()) {
            var itemStack = context.getItemInHand();
            var blockPos = context.getClickedPos();
            setTargetBlockPos(itemStack, blockPos);
            return InteractionResult.PASS; //TODO maybe SUCCESS
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack itemStack,
                                TooltipContext context,
                                List<Component> tooltipComponents,
                                TooltipFlag tooltipFlag) {
        var targetBlockPos = getTargetBlockPos(itemStack);

        if (targetBlockPos != null) {
            tooltipComponents.add(Component.literal("Pos: " + targetBlockPos));
        }

        super.appendHoverText(itemStack, context, tooltipComponents, tooltipFlag);
    }

    private void setTargetBlockPos(ItemStack itemStack, BlockPos targetBlockPos) {
        var customData = itemStack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        var customDataTag = customData.copyTag();

        if (targetBlockPos == null) {
            customDataTag.remove("SwapperTargetPos");
        } else {
            var targetBlockPosTag = NbtUtils.writeBlockPos(targetBlockPos);
            customDataTag.put("SwapperTargetPos", targetBlockPosTag);
        }

        var blockEntityType = Services.get(RegistryService.class).getBlockEntityType("swapper");

        BlockItem.setBlockEntityData(itemStack, blockEntityType, customDataTag);
    }

    private BlockPos getTargetBlockPos(ItemStack itemStack) {
        var customData = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (customData == null) {
            return null;
        }

        var customDataTag = customData.copyTag();

        if (customDataTag.contains("SwapperTargetPos")) {
            BlockPos targetBlockPos = NbtUtils.readBlockPos(customDataTag, "SwapperTargetPos").get();
            return targetBlockPos;
        } else {
            return null;
        }
    }

}
