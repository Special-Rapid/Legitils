package com.snkisk.hypixellegitils.loader;

import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
    private static final String MIXIN_ENVIRONMENT_CLASS = "org.spongepowered.asm.mixin.MixinEnvironment";
    private static final String MIXINS_CLASS = "org.spongepowered.asm.mixin.Mixins";
    static final int MIXIN_HOST_DIAGNOSTIC_LIMIT = 4;
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
            if (!appendModJarToSystemSearch(config, instrumentation)) {
                status("mod-jar-error");
                return;
            }
            System.setProperty(config.injectedProperty, "true");
            status("mod-jar-appended");
            // Lunar's current Genesis runtime creates its Ichor Mixin host after
            // premain. Register only once that host loader exists: forcing the
            // system-loader copy of MixinBootstrap earlier has no host service.
            new MixinRegistrationProbe(config, instrumentation).start();
        } catch (Throwable exception) {
            status("config-error");
            diagnostic("Loader disabled: " + exception.getClass().getSimpleName());
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
    private static boolean appendModJarToSystemSearch(LoaderConfig config, Instrumentation instrumentation) {
        JarFile jar = null;
        try {
            jar = new JarFile(config.modJar.toFile());
            publishBuildIdentity(jar);
            instrumentation.appendToSystemClassLoaderSearch(jar);
            systemSearchJar = jar;
            diagnostic("MOD JAR appended to the system classloader search.");
            return true;
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
            return false;
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
                ClassLoader mixinLoader = findPrelaunchMixinLoader();
                if (mixinLoader != null && MixinRuntimeRegistrar.register(config, mixinLoader)) {
                    status("mixin-config-registered");
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
            diagnostic("Mixin host candidates: " + describeMixinHostCandidates());
        }

        /**
         * Emits only bounded classloader implementation names and recognised Lunar
         * stage labels. It never includes thread names, paths, or raw descriptions.
         */
        private String describeMixinHostCandidates() {
            List<String> loadedMixins = new ArrayList<String>();
            List<String> loadedMinecraft = new ArrayList<String>();
            for (Class<?> candidate : instrumentation.getAllLoadedClasses()) {
                if ("org.spongepowered.asm.mixin.Mixins".equals(candidate.getName())) {
                    addCandidate(loadedMixins, candidate.getClassLoader());
                }
                if ("net.minecraft.client.Minecraft".equals(candidate.getName())) {
                    addCandidate(loadedMinecraft, candidate.getClassLoader());
                }
            }
            List<String> stageContexts = new ArrayList<String>();
            for (Thread thread : Thread.getAllStackTraces().keySet()) {
                ClassLoader candidate = thread.getContextClassLoader();
                if (isLabeledLunarGameMixinHost(candidate == null ? null : candidate.toString())) {
                    addCandidate(stageContexts, candidate);
                }
            }
            return "loadedMixins=" + loadedMixins
                + "; loadedMinecraft=" + loadedMinecraft
                + "; stageContexts=" + stageContexts;
        }

        private void addCandidate(List<String> candidates, ClassLoader loader) {
            addMixinHostCandidate(
                candidates,
                loader == null ? null : loader.getClass().getName(),
                loader == null ? null : loader.toString()
            );
        }

        /**
         * Match the prelaunch point used by the working Meowtils agent: the game
         * Mixin environment exists before Minecraft is defined. A META/PRE stage
         * is excluded and both MixinEnvironment and Mixins must share one loader.
         */
        private ClassLoader findPrelaunchMixinLoader() {
            for (Class<?> candidate : instrumentation.getAllLoadedClasses()) {
                if (!MIXIN_ENVIRONMENT_CLASS.equals(candidate.getName())) continue;
                ClassLoader loader = candidate.getClassLoader();
                if (!isPrelaunchMixinEnvironmentHost(loader) || !ownsMixinBootstrap(loader)) continue;
                try {
                    Class<?> mixins = Class.forName(MIXINS_CLASS, false, loader);
                    if (mixins.getClassLoader() == loader) return loader;
                } catch (Throwable ignored) {
                    // The environment can precede its facade class by one poll.
                }
            }
            return null;
        }

        private boolean ownsMixinBootstrap(ClassLoader candidate) {
            try {
                Class<?> bootstrap = Class.forName(MixinRuntimeRegistrar.bootstrapClassName(), false, candidate);
                return bootstrap.getClassLoader() == candidate;
            } catch (Throwable ignored) {
                // Genesis may expose the loader before its Mixin resources;
                // keep polling within the bounded compatibility window.
                return false;
            }
        }

        private boolean isPrelaunchMixinEnvironmentHost(ClassLoader candidate) {
            return candidate != null && HypixelLegitilsLoader.isNonMetaOrPreMixinHost(
                candidate.toString()
            );
        }

    }

    static boolean isIchorClassLoaderName(String className) {
        return className != null && className.startsWith("com.moonsworth.lunar.ichor.");
    }

    /**
     * Lunar exposes several Ichor loaders during launch. Only the normal MIXIN
     * stage owns game classes; PRE_OPTIFINE_PATCH has no host service and
     * META_MIXIN cannot see the Level Head renderer. Selecting either can make
     * registration timing depend on the number/order of Java agents.
     */
    static boolean isLunarGameMixinClassLoader(String className, String description) {
        return isIchorClassLoaderName(className)
            && isLabeledLunarGameMixinHost(description);
    }

    /**
     * The loaded-Mixins path already proves that this exact loader owns the Mixin
     * runtime. Some Genesis builds expose that loader with an obfuscated Java class
     * name while retaining the stable stage label in {@code toString()}; accept that
     * exact label only in the loaded-Mixins path, never during early discovery.
     */
    static boolean isLabeledLunarGameMixinHost(String description) {
        if (description == null || !description.contains("IchorClassLoader(MIXIN)")) return false;
        String normalized = description.toUpperCase(java.util.Locale.ROOT);
        return !normalized.contains("META_MIXIN")
            && !normalized.contains("PRE_OPTIFINE_PATCH");
    }

    static boolean isNonMetaOrPreMixinHost(String description) {
        if (description == null) return true;
        String normalized = description.toUpperCase(java.util.Locale.ROOT);
        return !normalized.contains("META_MIXIN")
            && !normalized.contains("PRE_OPTIFINE_PATCH");
    }

    static String describeMixinHostLoader(String className, String description) {
        String loader = className == null ? "bootstrap" : className;
        return loader + "[stage=" + lunarMixinStage(description) + "]";
    }

    static void addMixinHostCandidate(List<String> candidates, String className, String description) {
        String candidate = describeMixinHostLoader(className, description);
        if (!candidates.contains(candidate) && candidates.size() < MIXIN_HOST_DIAGNOSTIC_LIMIT) {
            candidates.add(candidate);
        }
    }

    private static String lunarMixinStage(String description) {
        if (description == null) return "unlabeled";
        if (description.contains("IchorClassLoader(PRE_OPTIFINE_PATCH)")) return "PRE_OPTIFINE_PATCH";
        if (description.contains("IchorClassLoader(META_MIXIN)")) return "META_MIXIN";
        if (description.contains("IchorClassLoader(MIXIN)")) return "MIXIN";
        return "unlabeled";
    }

}
