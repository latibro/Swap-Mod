package com.latibro.minecraft.swap.platform;

import com.latibro.minecraft.swap.Constants;
import com.latibro.minecraft.swap.platform.services.IPlatformHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

// Service loaders are a built-in Java feature that allow us to locate implementations of an interface that vary from one
// environment to another. In the context of MultiLoader we use this feature to access a mock API in the common code that
// is swapped out for the platform specific implementation at runtime.
public class Services {

    private static final Map<Class<?>, Object> serviceCache = new HashMap<>();

    // In this example we provide a platform helper which provides information about what platform the mod is running on.
    // For example this can be used to check if the code is running on Forge vs Fabric, or to ask the modloader if another
    // mod is loaded.
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static <T> T get(Class<T> clazz) {
        if (serviceCache.containsKey(clazz)) {
            var service = (T) serviceCache.get(clazz);
            Constants.LOG.info("Reusing {} for service {}", service, clazz);
            return service;
        }

        var service = load(clazz);
        serviceCache.put(clazz, service);
        return service;
    }

    // This code is used to load a service for the current environment. Your implementation of the service must be defined
    // manually by including a text file in META-INF/services named with the fully qualified class name of the service.
    // Inside the file you should write the fully qualified class name of the implementation to load for the platform. For
    // example our file on Forge points to ForgePlatformHelper while Fabric points to FabricPlatformHelper.
    private static <T> T load(Class<T> clazz) {
        final T service = ServiceLoader.load(clazz)
                                       .findFirst()
                                       .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOG.info("Loaded {} for service {}", service, clazz);
        return service;
    }
}