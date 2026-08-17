package com.snkisk.hypixellegitils.loader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class MixinRuntimeRegistrarTest {
    @Test
    public void registersTheFixedConfigThroughAnAvailableMixinBootstrap() throws Exception {
        Path directory = Files.createTempDirectory("legitils-mixin-runtime-test");
        Path modJar = LoaderConfigTest.createModJar(directory, true, true);
        Path configFile = directory.resolve("loader-config.json");
        String json = "{\"schemaVersion\":1,\"modJar\":\"" + modJar.toString()
            + "\",\"mixinConfig\":\"mixins.hypixellegitils.json\",\"injectedProperty\":\"hypixellegitils.agent.injected\"}";
        Files.write(configFile, json.getBytes(StandardCharsets.UTF_8));
        FixtureBootstrap.initialized = false;
        FixtureMixins.configuration = null;

        assertTrue(MixinRuntimeRegistrar.register(
            LoaderConfig.load(configFile.toAbsolutePath()),
            FixtureBootstrap.class.getClassLoader(),
            FixtureBootstrap.class.getName(),
            FixtureMixins.class.getName()
        ));
        assertTrue(FixtureBootstrap.initialized);
        assertEquals("mixins.hypixellegitils.json", FixtureMixins.configuration);
    }

    public static final class FixtureBootstrap {
        static boolean initialized;

        public static void init() {
            initialized = true;
        }
    }

    public static final class FixtureMixins {
        static String configuration;

        public static void addConfiguration(String value) {
            configuration = value;
        }
    }
}
