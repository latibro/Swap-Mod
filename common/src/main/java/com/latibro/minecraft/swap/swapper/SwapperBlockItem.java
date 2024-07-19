package com.latibro.minecraft.swap.swapper;

import com.latibro.minecraft.swap.Constants;
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

import java.util.Arrays;
import java.util.List;

import static com.latibro.minecraft.swap.swapper.SwapperBlockEntity.BLACKLISTED_BLOCKS;

public class SwapperBlockItem extends BlockItem {

    public SwapperBlockItem(SwapperBlock block) {
        super(block, new Item.Properties());
    }

    //TODO inspiration https://fabricmc.net/2024/04/19/1205.html#:~:text=called%20%E2%80%9Creloadable%20registries%E2%80%9D.-,Item%20Components,-We%20skip%20the
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Constants.LOG.debug("SWAPPER useOn");

        var itemStack = context.getItemInHand();
        var customData = itemStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        var customDataTag = customData.copyTag();

        if (context.getPlayer().isCrouching()) {
            if (Arrays.stream(BLACKLISTED_BLOCKS).anyMatch(context.getLevel().getBlockState(context.getClickedPos()).getBlock()::equals)) {
                Constants.LOG.warn("SWAPPER target is blacklisted");
                context.getPlayer().displayClientMessage(Component.literal("Not a swappable target"), false);
                return InteractionResult.FAIL;
            }

            var blockPos = context.getClickedPos();
            var blockPosTag = NbtUtils.writeBlockPos(blockPos);
            customDataTag.put("SwapperTargetPos", blockPosTag);
            itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(customDataTag));

            return InteractionResult.PASS;
        } else {
            Constants.LOG.warn("SWAPPER pos "
                               + customDataTag
                               + " "
                               + context.getClickedPos().relative(context.getClickedFace(), 1));
            if (customDataTag.contains("SwapperTargetPos")) {
                BlockPos targetPos = NbtUtils.readBlockPos(customDataTag, "SwapperTargetPos").get();
                BlockPos clickedPos = context.getClickedPos().relative(context.getClickedFace(), 1);
                if (targetPos.equals(clickedPos)) {
                    Constants.LOG.warn("SWAPPER target and swapper is at same position");
                    context.getPlayer().displayClientMessage(Component.literal("Unable to place at target"), false);
                    return InteractionResult.FAIL;
                }
            }

            return super.useOn(context);
        }
    }

    @Override
    public void appendHoverText(ItemStack itemStack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        var customData = itemStack.get(DataComponents.CUSTOM_DATA);
        var customDataTag = customData.copyTag();
        if (customDataTag.contains("SwapperTargetPos")) {
            BlockPos targetPos = NbtUtils.readBlockPos(customDataTag, "SwapperTargetPos").get();
            tooltipComponents.add(Component.literal("Pos: " + targetPos));
        }

        super.appendHoverText(itemStack, context, tooltipComponents, tooltipFlag);
    }

}
