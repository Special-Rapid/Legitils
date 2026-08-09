package net.kyori.adventure.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Test-only component used to exercise the reflection boundary without Lunar. */
public final class LegitilsTestTextComponent implements Component {
    public final String content;
    private final List<Object> children;

    public LegitilsTestTextComponent(String content, List<Object> children) {
        this.content = content;
        this.children = Collections.unmodifiableList(new ArrayList<Object>(children));
    }

    @Override
    public List<Object> children() {
        return children;
    }

    @Override
    public Component children(List<Object> children) {
        return new LegitilsTestTextComponent(content, children);
    }
}
