package com.snkisk.hypixellegitils.stats;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Guards the tracked public reference against accidental removal from the distributable MOD JAR. */
public final class StarColorCodeReferenceTest {
    @Test
    public void packagesTheCompletePublicPrestigeReference() throws Exception {
        InputStream stream = StarColorCodeReferenceTest.class.getResourceAsStream("/hypixellegitils-star-color-code.json");
        assertTrue(stream != null);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int count;
        while ((count = stream.read(buffer)) >= 0) output.write(buffer, 0, count);
        stream.close();
        String json = new String(output.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"schemaVersion\": 1"));
        assertTrue(json.contains("\"levelsPerPrestige\": 100"));
        assertEquals(100, occurrences(json, "✫") + occurrences(json, "✪") + occurrences(json, "⚝")
            + occurrences(json, "✥") + occurrences(json, "✭"));
        assertTrue(json.contains("§f[100✫]"));
        assertTrue(json.contains("§9[§b1§f0000§c✭§4]"));
    }

    private static int occurrences(String value, String target) {
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(target, index)) >= 0) {
            count++;
            index += target.length();
        }
        return count;
    }
}
