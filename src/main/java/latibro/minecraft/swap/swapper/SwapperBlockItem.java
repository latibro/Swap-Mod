package latibro.minecraft.swap.swapper;

import latibro.minecraft.swap.SwapMod;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

import static latibro.minecraft.swap.swapper.SwapperBlockEntity.BLACKLISTED_BLOCKS;

public class SwapperBlockItem extends BlockItem {

    public SwapperBlockItem() {
        super(SwapMod.SWAPPER_BLOCK.get(), new Item.Properties().tab(ItemGroup.TAB_MISC));
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        if (context.getPlayer().isCrouching()) {
            if (Arrays.stream(BLACKLISTED_BLOCKS).anyMatch(context.getLevel().getBlockState(context.getClickedPos()).getBlock()::equals)) {
                SwapMod.LOGGER.warn("SWAPPER target is blacklisted");
                context.getPlayer().sendMessage(new StringTextComponent("Not a swappable target"), Util.NIL_UUID);
                return ActionResultType.FAIL;
            }

            CompoundNBT blockEntityTag = context.getItemInHand().getOrCreateTagElement("BlockEntityTag");
            blockEntityTag.put("SwapperTargetPos", NBTUtil.writeBlockPos(context.getClickedPos()));

            return ActionResultType.PASS;
        } else {
            CompoundNBT blockEntityTag = context.getItemInHand().getOrCreateTagElement("BlockEntityTag");
            CompoundNBT targetPosTag = blockEntityTag.getCompound("SwapperTargetPos");
            SwapMod.LOGGER.warn("SWAPPER pos " + targetPosTag + " " + context.getClickedPos().relative(context.getClickedFace(), 1));
            if (targetPosTag != null) {
                BlockPos targetPos = NBTUtil.readBlockPos(targetPosTag);
                BlockPos clickedPos = context.getClickedPos().relative(context.getClickedFace(), 1);
                if (targetPos.equals(clickedPos)) {
                    SwapMod.LOGGER.warn("SWAPPER target and swapper is at same position");
                    context.getPlayer().sendMessage(new StringTextComponent("Unable to place at target"), Util.NIL_UUID);
                    return ActionResultType.FAIL;
                }
            }

            return super.useOn(context);
        }
    }

    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        CompoundNBT blockEntityTag = itemStack.getTagElement("BlockEntityTag");
        if (blockEntityTag != null && blockEntityTag.contains("SwapperTargetPos")) {
            BlockPos targetPos = NBTUtil.readBlockPos(blockEntityTag.getCompound("SwapperTargetPos"));
            tooltip.add(new StringTextComponent("Pos: " + targetPos));
        }
    }
}
