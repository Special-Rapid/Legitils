package com.snkisk.hypixellegitils.loader;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * Registers the MOD's fixed Mixin config with Lunar's bundled Mixin runtime
 * during JVM startup. Lunar's current baked launch can defer loading Mixins
 * indefinitely, so observing only already-loaded classes is not sufficient.
 */
final class MixinRuntimeRegistrar {
    private static final String BOOTSTRAP_CLASS = "org.spongepowered.asm.launch.MixinBootstrap";
    private static final String MIXINS_CLASS = "org.spongepowered.asm.mixin.Mixins";

    private MixinRuntimeRegistrar() {
    }

    static boolean register(LoaderConfig config, ClassLoader loader) {
        return register(config, loader, BOOTSTRAP_CLASS, MIXINS_CLASS);
    }

    static String bootstrapClassName() {
        return BOOTSTRAP_CLASS;
    }

    static boolean register(LoaderConfig config, ClassLoader loader, String bootstrapClassName, String mixinsClassName) {
        if (config == null || loader == null) return false;
        try {
            Class<?> bootstrap = Class.forName(bootstrapClassName, true, loader);
            bootstrap.getMethod("init").invoke(null);
            Class<?> mixins = Class.forName(mixinsClassName, true, loader);
            ClassLoader mixinLoader = mixins.getClassLoader();
            if (mixinLoader != loader) {
                HypixelLegitilsLoader.diagnostic("Mixin runtime resolved from a different loader; registration skipped.");
                return false;
            }
            if (addJarUrl(mixinLoader, config.modJar.toFile().toURI().toURL())) {
                HypixelLegitilsLoader.diagnostic("MOD JAR added directly to the Mixin classloader.");
            } else {
                HypixelLegitilsLoader.diagnostic("Mixin classloader has no compatible addURL method; using the system classloader search.");
            }
            Method addConfiguration = mixins.getMethod("addConfiguration", String.class);
            addConfiguration.invoke(null, config.mixinConfig);
            return true;
        } catch (Throwable exception) {
            HypixelLegitilsLoader.diagnostic("Early Mixin registration unavailable: " + exception.getClass().getSimpleName());
            return false;
        }
    }

    static boolean addJarUrl(ClassLoader classLoader, URL url) {
        Class<?> current = classLoader.getClass();
        while (current != null) {
            try {
                Method addUrl = current.getDeclaredMethod("addURL", URL.class);
                addUrl.setAccessible(true);
                addUrl.invoke(classLoader, url);
                return true;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            } catch (Throwable exception) {
                HypixelLegitilsLoader.diagnostic("Unable to add MOD JAR to Mixin classloader: " + exception.getClass().getSimpleName());
                return false;
            }
        }
        return false;
    }
}
