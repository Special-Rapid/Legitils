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
    public void validatesTheHypixelKeyOnceWithoutSendingAnyKeyOrPlayerData() throws Exception {
        final AtomicInteger requests = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/v1/hypixel-key-validation", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws java.io.IOException {
                requests.incrementAndGet();
                assertEquals("POST", exchange.getRequestMethod());
                assertEquals("capability0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ", exchange.getRequestHeaders().getFirst("X-Legitils-Capability"));
                assertEquals("{\"schemaVersion\":1}", new String(readAll(exchange), StandardCharsets.UTF_8));
                byte[] body = "{\"schemaVersion\":1,\"status\":\"invalid\"}".getBytes(StandardCharsets.UTF_8);
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
        Path directory = Files.createTempDirectory("legitils-hypixel-key-validation");
        try {
            writeDescriptor(
                directory.resolve("stats-bridge.json"),
                server.getAddress().getPort(),
                "capability0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                System.currentTimeMillis() + 120000L
            );
            StatsBridgeClient client = new StatsBridgeClient(directory.resolve("stats-bridge.json"));
            assertEquals(HypixelKeyValidationResult.INVALID, client.requestHypixelKeyValidationOnce(System.currentTimeMillis()));
            client.resetForNewWorld();
            assertEquals(HypixelKeyValidationResult.ALREADY_REQUESTED, client.requestHypixelKeyValidationOnce(System.currentTimeMillis()));
            assertEquals(HypixelKeyValidationResult.INVALID, client.requestHypixelKeyValidationAfterKeyChange(System.currentTimeMillis()));
            assertEquals(2, requests.get());
        } finally {
            server.stop(0);
            deleteTree(directory);
        }
    }

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
    public void retainsOnlyABoundedRecentWindowOfRequestIds() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/v1/roster", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws java.io.IOException {
                readAll(exchange);
                byte[] body = readyResponse().getBytes(StandardCharsets.UTF_8);
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
        Path directory = Files.createTempDirectory("legitils-bounded-request-ids");
        try {
            writeDescriptor(
                directory.resolve("stats-bridge.json"),
                server.getAddress().getPort(),
                "capability0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                System.currentTimeMillis() + 120000L
            );
            StatsBridgeClient client = new StatsBridgeClient(directory.resolve("stats-bridge.json"));
            for (int index = 0; index < 256; index++) {
                assertEquals(StatsBridgeLookupResult.Status.READY, client.requestOnce(
                    "manual_" + index, BedwarsMode.FOURS, players(), System.currentTimeMillis()
                ).status);
            }
            assertEquals(256, client.retainedRequestIdCount());
            assertEquals(StatsBridgeLookupResult.Status.ALREADY_REQUESTED, client.requestOnce(
                "manual_0", BedwarsMode.FOURS, players(), System.currentTimeMillis()
            ).status);
            assertEquals(StatsBridgeLookupResult.Status.READY, client.requestOnce(
                "manual_256", BedwarsMode.FOURS, players(), System.currentTimeMillis()
            ).status);
            assertEquals(StatsBridgeLookupResult.Status.READY, client.requestOnce(
                "manual_1", BedwarsMode.FOURS, players(), System.currentTimeMillis()
            ).status);
            assertEquals(StatsBridgeLookupResult.Status.ALREADY_REQUESTED, client.requestOnce(
                "manual_0", BedwarsMode.FOURS, players(), System.currentTimeMillis()
            ).status);
            assertEquals(256, client.retainedRequestIdCount());
        } finally {
            server.stop(0);
            deleteTree(directory);
        }
    }

    @Test
    public void requestsStatsWithoutAModeWhenTheVisibleSidebarOmitsIt() throws Exception {
        final AtomicInteger requests = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/v1/roster", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws java.io.IOException {
                requests.incrementAndGet();
                String request = new String(readAll(exchange), StandardCharsets.UTF_8);
                assertFalse(request.contains("gameMode"));
                byte[] body = readyResponse().getBytes(StandardCharsets.UTF_8);
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
            StatsBridgeLookupResult result = new StatsBridgeClient(directory.resolve("stats-bridge.json")).requestOnce(
                "match_without_mode", BedwarsMode.UNKNOWN, players(), System.currentTimeMillis()
            );
            assertEquals(StatsBridgeLookupResult.Status.READY, result.status);
            assertEquals(1, requests.get());
        } finally {
            server.stop(0);
            deleteTree(directory);
        }
    }

    @Test
    public void waitsForTheCompanionProviderWindowInsteadOfFailingAfterOneSecond() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
        server.createContext("/v1/roster", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws java.io.IOException {
                readAll(exchange);
                try {
                    Thread.sleep(1200L);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new java.io.IOException("Interrupted while simulating Companion provider work", exception);
                }
                byte[] body = readyResponse().getBytes(StandardCharsets.UTF_8);
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
            StatsBridgeLookupResult result = new StatsBridgeClient(directory.resolve("stats-bridge.json")).requestOnce(
                "manual_wait_for_companion", BedwarsMode.FOURS, players(), System.currentTimeMillis()
            );
            assertEquals(StatsBridgeLookupResult.Status.READY, result.status);
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

    @Test
    public void acceptsOnlyBoundedSafeCommunityTagTooltips() throws Exception {
        StatsBridgeLookupResult accepted = request(
            "{\"schemaVersion\":2,\"availability\":\"ready\",\"players\":[{\"name\":\"Player_1\",\"nickStatus\":\"known\",\"communityTags\":[{\"source\":\"urchin\",\"label\":\"Closet Cheater\",\"tooltip\":\"vape v4\\n- Added by @hexze\"}]}]}"
        );
        assertEquals(StatsBridgeLookupResult.Status.READY, accepted.status);
        assertEquals("vape v4\n- Added by @hexze", accepted.players.get(0).communityTags.get(0).tooltip);

        StatsBridgeLookupResult confirmed = request(
            "{\"schemaVersion\":2,\"availability\":\"ready\",\"players\":[{\"name\":\"Player_1\",\"nickStatus\":\"known\",\"communityTags\":[{\"source\":\"seraph\",\"label\":\"Confirmed Closet Cheating\",\"tooltip\":\"confirmed by Seraph\"}]}]}"
        );
        assertEquals(StatsBridgeLookupResult.Status.READY, confirmed.status);
        assertEquals("Confirmed Closet Cheating", confirmed.players.get(0).communityTags.get(0).label);

        StatsBridgeLookupResult unsafe = request(
            "{\"schemaVersion\":2,\"availability\":\"ready\",\"players\":[{\"name\":\"Player_1\",\"nickStatus\":\"known\",\"communityTags\":[{\"source\":\"urchin\",\"label\":\"Closet Cheater\",\"tooltip\":\"unsafe\\u00a7cformat\"}]}]}"
        );
        assertEquals(StatsBridgeLookupResult.Status.UNAVAILABLE, unsafe.status);

        StatsBridgeLookupResult unknownLabel = request(
            "{\"schemaVersion\":2,\"availability\":\"ready\",\"players\":[{\"name\":\"Player_1\",\"nickStatus\":\"known\",\"communityTags\":[{\"source\":\"urchin\",\"label\":\"unreviewed provider field\"}]}]}"
        );
        assertEquals(StatsBridgeLookupResult.Status.UNAVAILABLE, unknownLabel.status);
    }

    @Test
    public void measuresTooltipBoundsInUtf16CodeUnitsLikeTheCompanion() throws Exception {
        StringBuilder acceptedTooltip = new StringBuilder();
        for (int index = 0; index < 192; index++) acceptedTooltip.append("\uD83D\uDE00");
        assertEquals(384, acceptedTooltip.length());
        StatsBridgeLookupResult accepted = request(
            "{\"schemaVersion\":2,\"availability\":\"ready\",\"players\":[{\"name\":\"Player_1\",\"nickStatus\":\"known\",\"communityTags\":[{\"source\":\"urchin\",\"label\":\"Legit Sniper\",\"tooltip\":\""
                + acceptedTooltip + "\"}]}]}"
        );
        assertEquals(StatsBridgeLookupResult.Status.READY, accepted.status);

        StringBuilder rejectedTooltip = new StringBuilder(acceptedTooltip);
        rejectedTooltip.append("\uD83D\uDE00");
        assertEquals(386, rejectedTooltip.length());
        StatsBridgeLookupResult rejected = request(
            "{\"schemaVersion\":2,\"availability\":\"ready\",\"players\":[{\"name\":\"Player_1\",\"nickStatus\":\"known\",\"communityTags\":[{\"source\":\"urchin\",\"label\":\"Legit Sniper\",\"tooltip\":\""
                + rejectedTooltip + "\"}]}]}"
        );
        assertEquals(StatsBridgeLookupResult.Status.UNAVAILABLE, rejected.status);
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
        return "{\"schemaVersion\":2,\"availability\":\"ready\",\"players\":[{\"name\":\"Player_1\",\"nickStatus\":\"known\",\"stars\":100,\"finalKillDeathRatio\":5.0,\"modeWinStreak\":10,\"communityTags\":[{\"source\":\"urchin\",\"label\":\"Legit Sniper\",\"tooltip\":\"queued repeatedly\"}]}]}";
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
