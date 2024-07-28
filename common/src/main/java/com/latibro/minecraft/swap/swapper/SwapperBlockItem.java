package com.latibro.minecraft.swap.swapper;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class SwapperBlockItem extends BlockItem {

    public SwapperBlockItem(SwapperBlock block) {
        super(block, new Item.Properties());
    }

}
