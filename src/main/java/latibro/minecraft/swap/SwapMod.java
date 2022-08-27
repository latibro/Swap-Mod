package latibro.minecraft.swap;

import latibro.minecraft.swap.swapper.SwapperBlock;
import latibro.minecraft.swap.swapper.SwapperBlockEntity;
import latibro.minecraft.swap.swapper.SwapperBlockItem;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SwapMod.MODID)
public class SwapMod {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MODID = "swap";

    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    private static final DeferredRegister<TileEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, MODID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public static final RegistryObject<Block> SWAPPER_BLOCK = BLOCKS.register("swapper", SwapperBlock::new);
    public static final RegistryObject<TileEntityType<SwapperBlockEntity>> SWAPPER_BLOCK_ENTITY = BLOCK_ENTITIES.register(
            "swapper", () -> TileEntityType.Builder.of(SwapperBlockEntity::new, SWAPPER_BLOCK.get()).build(null)
    );
    public static final RegistryObject<Item> SWAPPER_ITEM = ITEMS.register(SWAPPER_BLOCK.getId().getPath(), SwapperBlockItem::new);

    public SwapMod() {
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        BLOCK_ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }
}
