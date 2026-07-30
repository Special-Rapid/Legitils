package com.snkisk.hypixellegitils.profile;

import java.io.IOException;
import java.util.UUID;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MojangProfileResolverTest {
    @Test
    public void parsesTheOfficialCompactUuidResponse() {
        MojangProfileResolver resolver = new MojangProfileResolver(new MojangProfileResolver.Transport() {
            @Override
            public MojangProfileResolver.Response get(String name) {
                return new MojangProfileResolver.Response(200, "{\"id\":\"069a79f444e94726a5befca90e38aaf5\",\"name\":\"Notch\"}");
            }
        });
        MojangProfileResolver.Resolution result = resolver.resolve("Notch");
        assertEquals(MojangProfileResolver.Status.FOUND, result.status);
        assertEquals(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), result.playerId);
        assertEquals("Notch", result.canonicalName);
    }

    @Test
    public void treatsNotFoundAndUnavailableResponsesSeparatelyWithoutRetrying() {
        MojangProfileResolver notFound = new MojangProfileResolver(new MojangProfileResolver.Transport() {
            @Override
            public MojangProfileResolver.Response get(String name) {
                return new MojangProfileResolver.Response(404, "{\"errorMessage\":\"missing\"}");
            }
        });
        assertEquals(MojangProfileResolver.Status.NOT_FOUND, notFound.resolve("MissingName").status);
        MojangProfileResolver unavailable = new MojangProfileResolver(new MojangProfileResolver.Transport() {
            @Override
            public MojangProfileResolver.Response get(String name) throws IOException {
                throw new IOException("offline");
            }
        });
        MojangProfileResolver.Resolution result = unavailable.resolve("Notch");
        assertEquals(MojangProfileResolver.Status.UNAVAILABLE, result.status);
        assertNull(result.playerId);
    }
}
