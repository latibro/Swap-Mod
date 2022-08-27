package latibro.minecraft.swap.swapper;

import latibro.minecraft.swap.SwapMod;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class SwapperBlockItem extends BlockItem {

    public SwapperBlockItem() {
        super(SwapMod.SWAPPER_BLOCK.get(), new Item.Properties().tab(ItemGroup.TAB_MISC));
    }

    @Override
    public ActionResultType useOn(ItemUseContext context) {
        if (context.getPlayer().isCrouching()) {
            CompoundNBT blockEntityTag = context.getItemInHand().getOrCreateTagElement("BlockEntityTag");
            blockEntityTag.put("SwapperTargetPos", NBTUtil.writeBlockPos(context.getClickedPos()));

            return ActionResultType.PASS;
        } else {
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
