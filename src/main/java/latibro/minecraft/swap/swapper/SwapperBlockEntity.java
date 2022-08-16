package latibro.minecraft.swap.swapper;

import net.minecraft.tileentity.TileEntity;

import static latibro.minecraft.swap.SwapMod.SWAPPER_BLOCK_ENTITY;

public class SwapperBlockEntity extends TileEntity {
    public SwapperBlockEntity() {
        super(SWAPPER_BLOCK_ENTITY.get());
    }
}
