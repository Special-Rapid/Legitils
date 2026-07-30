package com.snkisk.hypixellegitils.profile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;

/** Resolves an explicitly supplied Java player name to a UUID through Mojang's profile API. */
public final class MojangProfileResolver {
    private static final String ENDPOINT = "https://api.mojang.com/users/profiles/minecraft/";
    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    private static final int READ_TIMEOUT_MILLIS = 3000;
    private static final int MAXIMUM_RESPONSE_BYTES = 16384;
    private static final Pattern PROFILE_ID = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"([0-9a-fA-F]{32})\\\"");
    private static final Pattern PROFILE_NAME = Pattern.compile("\\\"name\\\"\\s*:\\s*\\\"([A-Za-z0-9_]{1,16})\\\"");
    private final Transport transport;

    public MojangProfileResolver() {
        this(new HttpsTransport());
    }

    public MojangProfileResolver(Transport transport) {
        if (transport == null) throw new IllegalArgumentException("Transport is required");
        this.transport = transport;
    }

    public Resolution resolve(String requestedName) {
        if (!isValidPlayerName(requestedName)) return Resolution.invalidName();
        try {
            Response response = transport.get(requestedName);
            if (response.statusCode == HttpURLConnection.HTTP_NOT_FOUND) return Resolution.notFound();
            if (response.statusCode != HttpURLConnection.HTTP_OK) return Resolution.unavailable();
            Matcher id = PROFILE_ID.matcher(response.body);
            Matcher name = PROFILE_NAME.matcher(response.body);
            if (!id.find() || !name.find()) return Resolution.unavailable();
            return Resolution.found(toUuid(id.group(1)), name.group(1));
        } catch (Exception ignored) {
            return Resolution.unavailable();
        }
    }

    private static boolean isValidPlayerName(String value) {
        return value != null && value.matches("[A-Za-z0-9_]{1,16}");
    }

    private static UUID toUuid(String compact) {
        return UUID.fromString(
            compact.substring(0, 8) + "-" + compact.substring(8, 12) + "-" + compact.substring(12, 16)
                + "-" + compact.substring(16, 20) + "-" + compact.substring(20)
        );
    }

    public interface Transport {
        Response get(String playerName) throws IOException;
    }

    public static final class Response {
        public final int statusCode;
        public final String body;

        public Response(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body == null ? "" : body;
        }
    }

    public static final class Resolution {
        public final Status status;
        public final UUID playerId;
        public final String canonicalName;

        private Resolution(Status status, UUID playerId, String canonicalName) {
            this.status = status;
            this.playerId = playerId;
            this.canonicalName = canonicalName;
        }

        private static Resolution found(UUID playerId, String canonicalName) {
            return new Resolution(Status.FOUND, playerId, canonicalName);
        }

        private static Resolution notFound() {
            return new Resolution(Status.NOT_FOUND, null, null);
        }

        private static Resolution invalidName() {
            return new Resolution(Status.INVALID_NAME, null, null);
        }

        private static Resolution unavailable() {
            return new Resolution(Status.UNAVAILABLE, null, null);
        }
    }

    public enum Status {
        FOUND,
        NOT_FOUND,
        INVALID_NAME,
        UNAVAILABLE
    }

    private static final class HttpsTransport implements Transport {
        @Override
        public Response get(String playerName) throws IOException {
            HttpsURLConnection connection = (HttpsURLConnection) new URL(ENDPOINT + playerName).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setUseCaches(false);
            try {
                int status = connection.getResponseCode();
                InputStream stream = status == HttpURLConnection.HTTP_OK ? connection.getInputStream() : connection.getErrorStream();
                return new Response(status, readLimited(stream));
            } finally {
                connection.disconnect();
            }
        }

        private static String readLimited(InputStream stream) throws IOException {
            if (stream == null) return "";
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int read;
                while ((read = stream.read(buffer)) != -1) {
                    if (output.size() + read > MAXIMUM_RESPONSE_BYTES) throw new IOException("Response too large");
                    output.write(buffer, 0, read);
                }
                return new String(output.toByteArray(), "UTF-8");
            } finally {
                stream.close();
            }
        }
    }
}
