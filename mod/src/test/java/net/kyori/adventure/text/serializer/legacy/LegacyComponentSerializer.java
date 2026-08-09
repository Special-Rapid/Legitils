package net.kyori.adventure.text.serializer.legacy;

/** Minimal test double for Lunar's legacy-section serializer. */
public final class LegacyComponentSerializer {
    public static LegacyComponentSerializer legacySection() {
        return new LegacyComponentSerializer();
    }

    public Object deserialize(String text) {
        return new LegacyText(text);
    }

    public static final class LegacyText {
        public final String text;

        LegacyText(String text) {
            this.text = text;
        }
    }
}
