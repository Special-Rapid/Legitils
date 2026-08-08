package com.snkisk.hypixellegitils.stats;

import com.snkisk.hypixellegitils.config.SimpleJson;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Bounded, capability-protected local client. It never contacts a remote stats provider itself. */
public final class StatsBridgeClient {
    private static final int MAXIMUM_PLAYERS = 64;
    private static final int MAXIMUM_RESPONSE_BYTES = 32 * 1024;
    private static final Pattern MATCH_ID = Pattern.compile("[A-Za-z0-9_-]{1,80}");
    private static final Pattern TAG_SOURCE = Pattern.compile("[A-Za-z0-9_-]{1,24}");
    private static final int MAXIMUM_TAG_LABEL_LENGTH = 64;
    private final Path descriptorPath;
    private final Set<String> requestedMatchIds = Collections.synchronizedSet(new LinkedHashSet<String>());

    public StatsBridgeClient(Path descriptorPath) {
        this.descriptorPath = descriptorPath;
    }

    public StatsBridgeLookupResult requestOnce(
        String matchId,
        BedwarsMode gameMode,
        List<StatsBridgeRosterMember> players,
        long nowMillis
    ) {
        if (!isValidRequest(matchId, gameMode, players)) return StatsBridgeLookupResult.unavailable();
        Optional<StatsBridgeDescriptor> descriptor = StatsBridgeDescriptor.read(descriptorPath, nowMillis);
        if (!descriptor.isPresent()) return StatsBridgeLookupResult.unavailable();
        synchronized (requestedMatchIds) {
            if (!requestedMatchIds.add(matchId)) return StatsBridgeLookupResult.alreadyRequested();
        }
        return request(descriptor.get(), matchId, gameMode, players);
    }

    public void resetForNewWorld() {
        requestedMatchIds.clear();
    }

    private StatsBridgeLookupResult request(
        StatsBridgeDescriptor descriptor,
        String matchId,
        BedwarsMode gameMode,
        List<StatsBridgeRosterMember> players
    ) {
        HttpURLConnection connection = null;
        try {
            URL endpoint = new URL("http", "127.0.0.1", descriptor.port, "/v1/roster");
            connection = (HttpURLConnection) endpoint.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(750);
            connection.setReadTimeout(1000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("X-Legitils-Capability", descriptor.capability);
            byte[] body = requestBody(matchId, gameMode, players).getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            OutputStream output = connection.getOutputStream();
            try {
                output.write(body);
            } finally {
                output.close();
            }
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) return StatsBridgeLookupResult.unavailable();
            return parseResponse(readBounded(connection.getInputStream()));
        } catch (Exception exception) {
            return StatsBridgeLookupResult.unavailable();
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static boolean isValidRequest(String matchId, BedwarsMode gameMode, List<StatsBridgeRosterMember> players) {
        if (matchId == null || !MATCH_ID.matcher(matchId).matches() || players == null
            || players.isEmpty() || players.size() > MAXIMUM_PLAYERS) {
            return false;
        }
        for (StatsBridgeRosterMember player : players) {
            if (player == null || !player.isValid()) return false;
        }
        return true;
    }

    private static String requestBody(String matchId, BedwarsMode gameMode, List<StatsBridgeRosterMember> players) {
        Map<String, Object> request = new LinkedHashMap<String, Object>();
        request.put("schemaVersion", Integer.valueOf(StatsBridgeDescriptor.SCHEMA_VERSION));
        request.put("matchID", matchId);
        // Current Bed Wars sidebars can omit their mode. Keep the request useful
        // for stars/FKDR and omit only the mode-specific win streak in that case.
        if (gameMode != null && gameMode != BedwarsMode.UNKNOWN && gameMode.bridgeValue != null) {
            request.put("gameMode", gameMode.bridgeValue);
        }
        List<Object> members = new ArrayList<Object>();
        for (StatsBridgeRosterMember player : players) {
            Map<String, Object> member = new LinkedHashMap<String, Object>();
            member.put("name", player.name);
            member.put("uuid", player.uuid);
            members.add(member);
        }
        request.put("players", members);
        return SimpleJson.write(request);
    }

    private static byte[] readBounded(InputStream stream) throws Exception {
        if (stream == null) throw new IllegalArgumentException("Response stream is unavailable");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[2048];
        int read;
        while ((read = stream.read(buffer)) != -1) {
            if (output.size() + read > MAXIMUM_RESPONSE_BYTES) throw new IllegalArgumentException("Response is too large");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static StatsBridgeLookupResult parseResponse(byte[] raw) {
        try {
            Object parsed = SimpleJson.parse(new String(raw, StandardCharsets.UTF_8));
            if (!(parsed instanceof Map)) return StatsBridgeLookupResult.unavailable();
            Map<?, ?> response = (Map<?, ?>) parsed;
            if (response.size() != 3 || number(response.get("schemaVersion")) == null
                || number(response.get("schemaVersion")).intValue() != StatsBridgeDescriptor.SCHEMA_VERSION
                || !(response.get("availability") instanceof String)
                || !(response.get("players") instanceof List)) {
                return StatsBridgeLookupResult.unavailable();
            }
            String availability = (String) response.get("availability");
            if ("unavailable".equals(availability)) return StatsBridgeLookupResult.unavailable();
            if (!"ready".equals(availability)) return StatsBridgeLookupResult.unavailable();
            List<?> rawPlayers = (List<?>) response.get("players");
            if (rawPlayers.size() > MAXIMUM_PLAYERS) return StatsBridgeLookupResult.unavailable();
            List<StatsBridgePlayerResult> players = new ArrayList<StatsBridgePlayerResult>();
            for (Object rawPlayer : rawPlayers) {
                StatsBridgePlayerResult player = parsePlayer(rawPlayer);
                if (player == null) return StatsBridgeLookupResult.unavailable();
                players.add(player);
            }
            return StatsBridgeLookupResult.ready(players);
        } catch (RuntimeException exception) {
            return StatsBridgeLookupResult.unavailable();
        }
    }

    private static StatsBridgePlayerResult parsePlayer(Object rawPlayer) {
        if (!(rawPlayer instanceof Map)) return null;
        Map<?, ?> player = (Map<?, ?>) rawPlayer;
        if (player.size() < 3 || player.size() > 6 || !(player.get("name") instanceof String) || !(player.get("nickStatus") instanceof String)
            || !(player.get("communityTags") instanceof List)) {
            return null;
        }
        for (Object rawKey : player.keySet()) {
            if (!(rawKey instanceof String)) return null;
            String key = (String) rawKey;
            if (!"name".equals(key) && !"nickStatus".equals(key) && !"stars".equals(key)
                && !"finalKillDeathRatio".equals(key) && !"modeWinStreak".equals(key)
                && !"communityTags".equals(key)) {
                return null;
            }
        }
        StatsBridgeRosterMember identity = new StatsBridgeRosterMember((String) player.get("name"), null);
        if (!identity.isValid()) return null;
        StatsBridgePlayerResult.NickStatus nickStatus;
        try {
            nickStatus = StatsBridgePlayerResult.NickStatus.valueOf(((String) player.get("nickStatus")).toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
        Integer stars = boundedInteger(player.get("stars"), 0, 100000);
        Double fkdr = boundedDouble(player.get("finalKillDeathRatio"), 0D, 100000D);
        Integer winStreak = boundedInteger(player.get("modeWinStreak"), 0, 100000);
        if ((player.get("stars") != null && stars == null)
            || (player.get("finalKillDeathRatio") != null && fkdr == null)
            || (player.get("modeWinStreak") != null && winStreak == null)) {
            return null;
        }
        List<?> rawTags = (List<?>) player.get("communityTags");
        if (rawTags.size() > 16) return null;
        List<StatsBridgePlayerResult.CommunityTag> tags = new ArrayList<StatsBridgePlayerResult.CommunityTag>();
        for (Object rawTag : rawTags) {
            if (!(rawTag instanceof Map)) return null;
            Map<?, ?> tag = (Map<?, ?>) rawTag;
            if (tag.size() != 2 || !(tag.get("source") instanceof String) || !(tag.get("label") instanceof String)) return null;
            String source = (String) tag.get("source");
            String label = (String) tag.get("label");
            if (!TAG_SOURCE.matcher(source).matches() || label.length() < 1 || label.length() > MAXIMUM_TAG_LABEL_LENGTH) return null;
            tags.add(new StatsBridgePlayerResult.CommunityTag(source, label));
        }
        return new StatsBridgePlayerResult((String) player.get("name"), nickStatus, stars, fkdr, winStreak, tags);
    }

    private static Number number(Object value) {
        return value instanceof Number ? (Number) value : null;
    }

    private static Integer boundedInteger(Object value, int minimum, int maximum) {
        if (!(value instanceof Number)) return null;
        long number = ((Number) value).longValue();
        if (number < minimum || number > maximum || ((Number) value).doubleValue() != (double) number) return null;
        return Integer.valueOf((int) number);
    }

    private static Double boundedDouble(Object value, double minimum, double maximum) {
        if (!(value instanceof Number)) return null;
        double number = ((Number) value).doubleValue();
        if (Double.isNaN(number) || Double.isInfinite(number) || number < minimum || number > maximum) return null;
        return Double.valueOf(number);
    }
}
