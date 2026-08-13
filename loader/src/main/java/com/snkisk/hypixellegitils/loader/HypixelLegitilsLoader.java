package com.snkisk.hypixellegitils.loader;

import java.io.File;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Pre-launch compatibility spike only. This class never attaches to a running JVM,
 * installs a transformer, or changes packets/input/gameplay.
 */
public final class HypixelLegitilsLoader {
    private static final String STATUS_PROPERTY = "hypixellegitils.loader.status";
    private static final String BUILD_METADATA_ENTRY = "hypixellegitils-build.properties";
    private static final String BUILD_VERSION_PROPERTY = "hypixellegitils.build.version";
    private static final String BUILD_REVISION_PROPERTY = "hypixellegitils.build.revision";
    private static volatile JarFile systemSearchJar;

    private HypixelLegitilsLoader() {
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        try {
            if (!Lunar189Runtime.isSupported()) {
                status("unsupported-minecraft-version");
                diagnostic("Loader disabled: this build supports Lunar Minecraft 1.8.9 only.");
                return;
            }
            Path configPath = agentArgs == null ? null : Paths.get(agentArgs).normalize();
            LoaderConfig config = LoaderConfig.load(configPath);
            System.setProperty(config.injectedProperty, "true");
            status("config-valid");
            appendModJarToSystemSearch(config, instrumentation);
            new MixinRegistrationProbe(config, instrumentation).start();
        } catch (Exception exception) {
            status("config-error");
            diagnostic("Loader disabled: " + exception.getMessage());
        }
    }

    static void status(String value) {
        System.setProperty(STATUS_PROPERTY, value);
        diagnostic("status=" + value);
    }

    static void diagnostic(String message) {
        System.err.println("[HypixelLegitils Loader] " + message);
    }

    /**
     * Makes the MOD resources visible to a child Mixin classloader without
     * requiring that classloader to expose a reflective addURL method. This is
     * still a pre-launch classpath operation; it does not transform or redefine
     * any class.
     */
    private static void appendModJarToSystemSearch(LoaderConfig config, Instrumentation instrumentation) {
        JarFile jar = null;
        try {
            jar = new JarFile(config.modJar.toFile());
            publishBuildIdentity(jar);
            instrumentation.appendToSystemClassLoaderSearch(jar);
            systemSearchJar = jar;
            diagnostic("MOD JAR appended to the system classloader search.");
        } catch (Exception exception) {
            if (jar != null) {
                try {
                    jar.close();
                } catch (Exception ignored) {
                    // The loader remains optional if the diagnostic cleanup fails.
                }
            }
            diagnostic("Unable to append MOD JAR to the system classloader search: "
                + exception.getClass().getSimpleName());
        }
    }

    /**
     * A Mixin child loader can load MOD classes while refusing resource lookup from
     * the MOD JAR. Publish the already-packaged identity through the JVM boundary
     * so the MOD and Companion report the same injected build.
     */
    static void publishBuildIdentity(JarFile jar) {
        if (jar == null || jar.getJarEntry(BUILD_METADATA_ENTRY) == null) return;
        InputStream input = null;
        try {
            input = jar.getInputStream(jar.getJarEntry(BUILD_METADATA_ENTRY));
            Properties properties = new Properties();
            properties.load(input);
            setBuildProperty(BUILD_VERSION_PROPERTY, properties.getProperty("version"));
            setBuildProperty(BUILD_REVISION_PROPERTY, properties.getProperty("revision"));
        } catch (Exception exception) {
            diagnostic("Unable to publish MOD build identity: " + exception.getClass().getSimpleName());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ignored) {
                    // The build identity is diagnostic only.
                }
            }
        }
    }

    private static void setBuildProperty(String name, String value) {
        if (value != null && !value.trim().isEmpty()) System.setProperty(name, value.trim());
    }

    private static final class MixinRegistrationProbe implements Runnable {
        private static final long TIMEOUT_MILLIS = 30000L;
        private final LoaderConfig config;
        private final Instrumentation instrumentation;
        private final AtomicBoolean started = new AtomicBoolean(false);

        MixinRegistrationProbe(LoaderConfig config, Instrumentation instrumentation) {
            this.config = config;
            this.instrumentation = instrumentation;
        }

        void start() {
            if (!started.compareAndSet(false, true)) {
                return;
            }
            Thread thread = new Thread(this, "hypixel-legitils-mixin-probe");
            thread.setDaemon(true);
            thread.start();
        }

        @Override
        public void run() {
            long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
            while (System.currentTimeMillis() < deadline) {
                Class<?> mixins = findLoadedMixinsClass();
                if (mixins != null) {
                    register(mixins);
                    return;
                }
                try {
                    Thread.sleep(25L);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    status("interrupted");
                    return;
                }
            }
            status("mixin-runtime-timeout");
            diagnostic("Mixin runtime was not observed before the compatibility-spike timeout.");
        }

        private Class<?> findLoadedMixinsClass() {
            for (Class<?> candidate : instrumentation.getAllLoadedClasses()) {
                if ("org.spongepowered.asm.mixin.Mixins".equals(candidate.getName())) {
                    return candidate;
                }
            }
            return null;
        }

        private void register(Class<?> mixins) {
            try {
                ClassLoader mixinLoader = mixins.getClassLoader();
                if (mixinLoader == null) {
                    status("unsupported-bootstrap-loader");
                    diagnostic("Mixin runtime uses the bootstrap loader; MOD JAR cannot be added safely.");
                    return;
                }
                if (addJarUrl(mixinLoader, config.modJar.toFile().toURI().toURL())) {
                    diagnostic("MOD JAR added directly to the Mixin classloader.");
                } else {
                    diagnostic("Mixin classloader has no compatible addURL method; using the system classloader search.");
                }
                Method addConfiguration = mixins.getMethod("addConfiguration", String.class);
                addConfiguration.invoke(null, config.mixinConfig);
                status("mixin-config-registered");
            } catch (Exception exception) {
                status("mixin-registration-error");
                diagnostic("Mixin registration failed: " + exception.getClass().getSimpleName() + ": " + exception.getMessage());
            }
        }

        private boolean addJarUrl(ClassLoader classLoader, URL url) {
            Class<?> current = classLoader.getClass();
            while (current != null) {
                try {
                    Method addUrl = current.getDeclaredMethod("addURL", URL.class);
                    addUrl.setAccessible(true);
                    addUrl.invoke(classLoader, url);
                    return true;
                } catch (NoSuchMethodException ignored) {
                    current = current.getSuperclass();
                } catch (Exception exception) {
                    diagnostic("Unable to add MOD JAR to Mixin classloader: " + exception.getClass().getSimpleName());
                    return false;
                }
            }
            return false;
        }
    }
}
