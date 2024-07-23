package com.latibro.minecraft.swap;

import com.latibro.minecraft.swap.platform.Services;
import com.latibro.minecraft.swap.platform.services.RegistryService;
import com.latibro.minecraft.swap.swapper.SwapperBlock;
import com.latibro.minecraft.swap.swapper.SwapperBlockEntity;
import com.latibro.minecraft.swap.swapper.SwapperBlockItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;

// This class is part of the common project meaning it is shared between all supported loaders. Code written here can only
// import and access the vanilla codebase, libraries used by vanilla, and optionally third party libraries that provide
// common compatible binaries. This means common code can not directly use loader specific concepts such as Forge events
// however it will be compatible with all supported mod loaders.
public class CommonClass {

    //public static final SwapperBlock SWAPPER_BLOCK = new SwapperBlock();
    //public static final SwapperBlockItem SWAPPER_BLOCK_ITEM = new SwapperBlockItem(SWAPPER_BLOCK);

    //public static final BlockEntityType<SwapperBlockEntity> SWAPPER_BLOCK_ENTITY_TYPE =
    //            BlockEntityType.Builder.of(SwapperBlockEntity::new, SWAPPER_BLOCK).build(null);

    // The loader specific projects are able to import and use any code from the common project. This allows you to
    // write the majority of your code here and load it from your loader specific projects. This example has some
    // code that gets invoked by the entry point of the loader specific projects.
    public static void init() {

        Constants.LOG.info("Hello from Common init on {}! we are currently in a {} environment!",
                           Services.PLATFORM.getPlatformName(),
                           Services.PLATFORM.getEnvironmentName());
        Constants.LOG.info("The ID for diamonds is {}", BuiltInRegistries.ITEM.getKey(Items.DIAMOND));
        Constants.LOG.debug("The ID for gold is {}", BuiltInRegistries.ITEM.getKey(Items.GOLD_INGOT));

        var registryService = Services.get(RegistryService.class);

        var swapperBlockResourceName = "swapper";
        registryService.registerBlock(swapperBlockResourceName, SwapperBlock::new);
        registryService.registerItem(swapperBlockResourceName, () -> {
            var block = (SwapperBlock) registryService.getBlock(swapperBlockResourceName);
            return new SwapperBlockItem(block);
        });
        registryService.registerBlockEntityType(swapperBlockResourceName, () -> {
            var block = registryService.getBlock(swapperBlockResourceName);
            var builder = BlockEntityType.Builder.of(SwapperBlockEntity::new, block);
            //TODO figure out if dataType is needed for builder, like in BlockEntityType.register()
            return builder.build(null);
        });

        // It is common for all supported loaders to provide a similar feature that can not be used directly in the
        // common code. A popular way to get around this is using Java's built-in service loader feature to create
        // your own abstraction layer. You can learn more about this in our provided services class. In this example
        // we have an interface in the common code and use a loader specific implementation to delegate our call to
        // the platform specific approach.
        if (Services.PLATFORM.isModLoaded("swap")) {

            Constants.LOG.info("Hello to swap mod");
        }
    }
}