package com.snkisk.hypixellegitils.stats;

import com.snkisk.hypixellegitils.config.SimpleJson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class StatsBridgeClientTest {
    @Test
    public void missingOrExpiredDescriptorLeavesStatsUnavailable() throws Exception {
        Path directory = Files.createTempDirectory("legitils-stats-bridge");
        try {
            StatsBridgeClient client = new StatsBridgeClient(directory.resolve("stats-bridge.json"));
            assertEquals(StatsBridgeLookupResult.Status.UNAVAILABLE, client.requestOnce(
                "match_1", BedwarsMode.FOURS, players(), System.currentTimeMillis()
            ).status);

            writeDescriptor(directory.resolve("stats-bridge.json"), 12345, "aBcDeFgHiJkLmNoPqRsTuVwXyZ012345", System.currentTimeMillis() - 1L);
            assertEquals(StatsBridgeLookupResult.Status.UNAVAILABLE, client.requestOnce(
                "match_2", BedwarsMode.FOURS, players(), System.currentTimeMillis()
            ).status);
        } finally {
            deleteTree(directory);
        }
    }

    @Test
    public void requestsOnlyOnceAndAcceptsNormalizedResponse() throws Exception {
        final AtomicInteger requests = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/v1/roster", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws java.io.IOException {
                requests.incrementAndGet();
                assertEquals("POST", exchange.getRequestMethod());
                assertEquals("capability0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", exchange.getRequestHeaders().getFirst("X-Legitils-Capability"));
                String request = new String(readAll(exchange), StandardCharsets.UTF_8);
                assertFalse(request.contains("api-key"));
                assertTrue(request.contains("Player_1"));
                assertTrue(request.contains("\"gameMode\":\"four_four\""));
                byte[] body = readyResponse().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                OutputStream output = exchange.getResponseBody();
                try {
                    output.write(body);
                } finally {
                    output.close();
                }
            }
        });
        server.start();
        Path directory = Files.createTempDirectory("legitils-stats-bridge");
        try {
            writeDescriptor(
                directory.resolve("stats-bridge.json"),
                server.getAddress().getPort(),
                "capability0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                System.currentTimeMillis() + 120000L
            );
            StatsBridgeClient client = new StatsBridgeClient(directory.resolve("stats-bridge.json"));
            StatsBridgeLookupResult first = client.requestOnce("match_1", BedwarsMode.FOURS, players(), System.currentTimeMillis());
            assertEquals(StatsBridgeLookupResult.Status.READY, first.status);
            assertEquals(1, first.players.size());
            assertEquals("Player_1", first.players.get(0).name);
            assertEquals(StatsBridgePlayerResult.NickStatus.KNOWN, first.players.get(0).nickStatus);
            assertEquals(1, first.players.get(0).communityTags.size());
            assertEquals(StatsBridgeLookupResult.Status.ALREADY_REQUESTED, client.requestOnce(
                "match_1", BedwarsMode.FOURS, players(), System.currentTimeMillis()
            ).status);
            assertEquals(1, requests.get());
        } finally {
            server.stop(0);
            deleteTree(directory);
        }
    }

    @Test
    public void acceptsCompanionResponseWhenOptionalStatsAreOmitted() throws Exception {
        StatsBridgeLookupResult result = request(companionStyleReadyResponse());

        assertEquals(StatsBridgeLookupResult.Status.READY, result.status);
        assertEquals(1, result.players.size());
        assertEquals("Player_1", result.players.get(0).name);
        assertEquals(StatsBridgePlayerResult.NickStatus.KNOWN, result.players.get(0).nickStatus);
        assertEquals(null, result.players.get(0).stars);
        assertEquals(null, result.players.get(0).finalKillDeathRatio);
        assertEquals(null, result.players.get(0).modeWinStreak);
    }

    @Test
    public void rejectsUnexpectedPlayerResponseField() throws Exception {
        StatsBridgeLookupResult result = request(
            "{\"schemaVersion\":2,\"availability\":\"ready\",\"players\":[{\"name\":\"Player_1\",\"nickStatus\":\"known\",\"communityTags\":[],\"unexpected\":true}]}"
        );

        assertEquals(StatsBridgeLookupResult.Status.UNAVAILABLE, result.status);
    }

    private static List<StatsBridgeRosterMember> players() {
        List<StatsBridgeRosterMember> players = new ArrayList<StatsBridgeRosterMember>();
        players.add(new StatsBridgeRosterMember("Player_1", null));
        return players;
    }

    private static void writeDescriptor(Path path, int port, String capability, long expiry) throws Exception {
        Map<String, Object> descriptor = new LinkedHashMap<String, Object>();
        descriptor.put("schemaVersion", Integer.valueOf(StatsBridgeDescriptor.SCHEMA_VERSION));
        descriptor.put("port", Integer.valueOf(port));
        descriptor.put("capability", capability);
        descriptor.put("expiresAtEpochMillis", Long.valueOf(expiry));
        Files.write(path, SimpleJson.write(descriptor).getBytes(StandardCharsets.UTF_8));
    }

    private static String readyResponse() {
        return "{\"schemaVersion\":2,\"availability\":\"ready\",\"players\":[{\"name\":\"Player_1\",\"nickStatus\":\"known\",\"stars\":100,\"finalKillDeathRatio\":5.0,\"modeWinStreak\":10,\"communityTags\":[{\"source\":\"urchin\",\"label\":\"tag\"}]}]}";
    }

    private static String companionStyleReadyResponse() {
        return "{\"schemaVersion\":2,\"availability\":\"ready\",\"players\":[{\"name\":\"Player_1\",\"nickStatus\":\"known\",\"communityTags\":[]}]}";
    }

    private static StatsBridgeLookupResult request(final String response) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/v1/roster", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws java.io.IOException {
                readAll(exchange);
                byte[] body = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                OutputStream output = exchange.getResponseBody();
                try {
                    output.write(body);
                } finally {
                    output.close();
                }
            }
        });
        server.start();
        Path directory = Files.createTempDirectory("legitils-stats-bridge");
        try {
            writeDescriptor(
                directory.resolve("stats-bridge.json"),
                server.getAddress().getPort(),
                "capability0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                System.currentTimeMillis() + 120000L
            );
            return new StatsBridgeClient(directory.resolve("stats-bridge.json")).requestOnce(
                "match_1", BedwarsMode.FOURS, players(), System.currentTimeMillis()
            );
        } finally {
            server.stop(0);
            deleteTree(directory);
        }
    }

    private static byte[] readAll(HttpExchange exchange) throws java.io.IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = exchange.getRequestBody().read(buffer)) != -1) output.write(buffer, 0, read);
        return output.toByteArray();
    }

    private static void deleteTree(Path root) throws Exception {
        if (root == null || !Files.exists(root)) return;
        Files.walk(root)
            .sorted(java.util.Comparator.reverseOrder())
            .forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (java.io.IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
    }
}
