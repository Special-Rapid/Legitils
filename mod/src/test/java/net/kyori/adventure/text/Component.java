package net.kyori.adventure.text;

import java.util.List;

/** Minimal test double for Lunar's runtime Adventure interface. */
public interface Component extends TextComponent {
    List<Object> children();

    Component children(List<Object> children);
}
