package com.snkisk.hypixellegitils.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

final class LoaderConfig {
    private static final Pattern MIXIN_CONFIG = Pattern.compile("[A-Za-z0-9_.-]+\\.json");
    private static final Pattern SYSTEM_PROPERTY = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*");

    final Path modJar;
    final String mixinConfig;
    final String injectedProperty;

    private LoaderConfig(Path modJar, String mixinConfig, String injectedProperty) {
        this.modJar = modJar;
        this.mixinConfig = mixinConfig;
        this.injectedProperty = injectedProperty;
    }

    static LoaderConfig load(Path configPath) throws IOException, ConfigException {
        if (configPath == null || !configPath.isAbsolute() || !Files.isRegularFile(configPath)) {
            throw new ConfigException("loader config must be an existing absolute path");
        }

        Map<String, Object> values = new JsonObjectReader(new String(Files.readAllBytes(configPath), StandardCharsets.UTF_8)).read();
        if (values.size() != 4 || !values.keySet().contains("schemaVersion") || !values.keySet().contains("modJar")
                || !values.keySet().contains("mixinConfig") || !values.keySet().contains("injectedProperty")) {
            throw new ConfigException("loader config must contain exactly schemaVersion, modJar, mixinConfig, and injectedProperty");
        }

        Object schemaVersion = values.get("schemaVersion");
        if (!(schemaVersion instanceof Integer) || ((Integer) schemaVersion).intValue() != 1) {
            throw new ConfigException("unsupported loader config schemaVersion");
        }

        String modJarValue = requiredString(values, "modJar");
        Path modJar = java.nio.file.Paths.get(modJarValue).normalize();
        if (!modJar.isAbsolute() || !Files.isRegularFile(modJar) || !modJar.getFileName().toString().endsWith(".jar")) {
            throw new ConfigException("modJar must be an existing absolute .jar path");
        }

        String mixinConfig = requiredString(values, "mixinConfig");
        if (!MIXIN_CONFIG.matcher(mixinConfig).matches()) {
            throw new ConfigException("mixinConfig must be a root JAR JSON resource name");
        }
        validateModJarResources(modJar, mixinConfig);

        String injectedProperty = requiredString(values, "injectedProperty");
        if (!SYSTEM_PROPERTY.matcher(injectedProperty).matches()) {
            throw new ConfigException("injectedProperty is not a valid dotted Java system property name");
        }

        return new LoaderConfig(modJar, mixinConfig, injectedProperty);
    }

    private static void validateModJarResources(Path modJar, String mixinConfig) throws ConfigException {
        try {
            JarFile jar = new JarFile(modJar.toFile());
            try {
                if (jar.getJarEntry(mixinConfig) == null || jar.getJarEntry("hypixellegitils-build.properties") == null) {
                    throw new ConfigException("modJar is missing required MOD resources");
                }
            } finally {
                jar.close();
            }
        } catch (IOException exception) {
            throw new ConfigException("modJar must be a readable JAR with required MOD resources");
        }
    }

    private static String requiredString(Map<String, Object> values, String key) throws ConfigException {
        Object value = values.get(key);
        if (!(value instanceof String) || ((String) value).trim().isEmpty()) {
            throw new ConfigException(key + " must be a non-empty string");
        }
        return (String) value;
    }

    static final class ConfigException extends Exception {
        ConfigException(String message) {
            super(message);
        }
    }

    private static final class JsonObjectReader {
        private final String input;
        private int index;

        JsonObjectReader(String input) {
            this.input = input;
        }

        Map<String, Object> read() throws ConfigException {
            Map<String, Object> result = new HashMap<String, Object>();
            skipWhitespace();
            expect('{');
            skipWhitespace();
            if (consume('}')) {
                return result;
            }
            while (true) {
                String key = readString();
                if (result.containsKey(key)) {
                    throw error("duplicate key " + key);
                }
                skipWhitespace();
                expect(':');
                skipWhitespace();
                result.put(key, readValue());
                skipWhitespace();
                if (consume('}')) {
                    break;
                }
                expect(',');
                skipWhitespace();
            }
            skipWhitespace();
            if (index != input.length()) {
                throw error("unexpected content after object");
            }
            return result;
        }

        private Object readValue() throws ConfigException {
            if (peek() == '"') {
                return readString();
            }
            int start = index;
            while (index < input.length() && Character.isDigit(input.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw error("expected string or non-negative integer");
            }
            try {
                return Integer.valueOf(input.substring(start, index));
            } catch (NumberFormatException exception) {
                throw error("integer is out of range");
            }
        }

        private String readString() throws ConfigException {
            expect('"');
            StringBuilder result = new StringBuilder();
            while (index < input.length()) {
                char value = input.charAt(index++);
                if (value == '"') {
                    return result.toString();
                }
                if (value == '\\') {
                    if (index >= input.length()) {
                        throw error("unterminated escape");
                    }
                    char escaped = input.charAt(index++);
                    if (escaped == '"' || escaped == '\\' || escaped == '/') {
                        result.append(escaped);
                    } else if (escaped == 'b') {
                        result.append('\b');
                    } else if (escaped == 'f') {
                        result.append('\f');
                    } else if (escaped == 'n') {
                        result.append('\n');
                    } else if (escaped == 'r') {
                        result.append('\r');
                    } else if (escaped == 't') {
                        result.append('\t');
                    } else {
                        throw error("unsupported escape");
                    }
                } else if (value < 0x20) {
                    throw error("control character in string");
                } else {
                    result.append(value);
                }
            }
            throw error("unterminated string");
        }

        private char peek() throws ConfigException {
            if (index >= input.length()) {
                throw error("unexpected end of file");
            }
            return input.charAt(index);
        }

        private void expect(char expected) throws ConfigException {
            if (peek() != expected) {
                throw error("expected '" + expected + "'");
            }
            index++;
        }

        private boolean consume(char expected) {
            if (index < input.length() && input.charAt(index) == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void skipWhitespace() {
            while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
                index++;
            }
        }

        private ConfigException error(String message) {
            return new ConfigException(message + " at character " + index);
        }
    }
}
