package com.snkisk.hypixellegitils.loader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LoaderConfigTest {
    @Test
    public void acceptsTheDocumentedSchema() throws Exception {
        Path directory = Files.createTempDirectory("legitils-loader-test");
        Path modJar = createModJar(directory, true, true);
        Path config = directory.resolve("loader-config.json");
        String json = "{\"schemaVersion\":1,\"modJar\":\"" + modJar.toString() + "\",\"mixinConfig\":\"mixins.hypixellegitils.json\",\"injectedProperty\":\"hypixellegitils.agent.injected\"}";
        Files.write(config, json.getBytes(StandardCharsets.UTF_8));

        LoaderConfig parsed = LoaderConfig.load(config.toAbsolutePath());

        assertEquals(modJar, parsed.modJar);
        assertEquals("mixins.hypixellegitils.json", parsed.mixinConfig);
        assertEquals("hypixellegitils.agent.injected", parsed.injectedProperty);
    }

    @Test
    public void rejectsUnknownKeys() throws Exception {
        Path directory = Files.createTempDirectory("legitils-loader-test");
        Path modJar = createModJar(directory, true, true);
        Path config = directory.resolve("loader-config.json");
        String json = "{\"schemaVersion\":1,\"modJar\":\"" + modJar.toString() + "\",\"mixinConfig\":\"mixins.hypixellegitils.json\",\"injectedProperty\":\"hypixellegitils.agent.injected\",\"unexpected\":\"value\"}";
        Files.write(config, json.getBytes(StandardCharsets.UTF_8));

        try {
            LoaderConfig.load(config.toAbsolutePath());
            fail("Expected strict schema rejection");
        } catch (LoaderConfig.ConfigException expected) {
            assertEquals("loader config must contain exactly schemaVersion, modJar, mixinConfig, and injectedProperty", expected.getMessage());
        }
    }

    @Test
    public void rejectsAReadableJarThatCannotProvideTheConfiguredMixinResources() throws Exception {
        Path directory = Files.createTempDirectory("legitils-loader-test");
        Path modJar = createModJar(directory, true, false);
        Path config = directory.resolve("loader-config.json");
        String json = "{\"schemaVersion\":1,\"modJar\":\"" + modJar.toString()
            + "\",\"mixinConfig\":\"mixins.hypixellegitils.json\",\"injectedProperty\":\"hypixellegitils.agent.injected\"}";
        Files.write(config, json.getBytes(StandardCharsets.UTF_8));

        try {
            LoaderConfig.load(config.toAbsolutePath());
            fail("Expected missing MOD resource rejection");
        } catch (LoaderConfig.ConfigException expected) {
            assertEquals("modJar is missing required MOD resources", expected.getMessage());
        }
    }

    @Test
    public void rejectsAReadableJarThatCannotProvideTheConfiguredMixinJson() throws Exception {
        Path directory = Files.createTempDirectory("legitils-loader-test");
        Path modJar = createModJar(directory, false, true);
        Path config = directory.resolve("loader-config.json");
        String json = "{\"schemaVersion\":1,\"modJar\":\"" + modJar.toString()
            + "\",\"mixinConfig\":\"mixins.hypixellegitils.json\",\"injectedProperty\":\"hypixellegitils.agent.injected\"}";
        Files.write(config, json.getBytes(StandardCharsets.UTF_8));

        try {
            LoaderConfig.load(config.toAbsolutePath());
            fail("Expected missing Mixin JSON rejection");
        } catch (LoaderConfig.ConfigException expected) {
            assertEquals("modJar is missing required MOD resources", expected.getMessage());
        }
    }

    static Path createModJar(Path directory, boolean includeMixinConfig, boolean includeBuildMetadata) throws Exception {
        Path modJar = directory.resolve("hypixel-legitils.jar");
        JarOutputStream output = new JarOutputStream(Files.newOutputStream(modJar));
        try {
            if (includeMixinConfig) {
                output.putNextEntry(new JarEntry("mixins.hypixellegitils.json"));
                output.write("{}".getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
            if (includeBuildMetadata) {
                output.putNextEntry(new JarEntry("hypixellegitils-build.properties"));
                output.write("version=test\nrevision=test\n".getBytes(StandardCharsets.UTF_8));
                output.closeEntry();
            }
        } finally {
            output.close();
        }
        return modJar;
    }
}
