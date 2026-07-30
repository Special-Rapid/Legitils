package com.snkisk.hypixellegitils.loader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LoaderConfigTest {
    @Test
    public void acceptsTheDocumentedSchema() throws Exception {
        Path directory = Files.createTempDirectory("legitils-loader-test");
        Path modJar = Files.createFile(directory.resolve("hypixel-legitils.jar"));
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
        Path modJar = Files.createFile(directory.resolve("hypixel-legitils.jar"));
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
}
