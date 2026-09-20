package org.jxsd.rendering;

import org.jxsd.model.Compositor;

/**
 * How a group is drawn: a model group renders its compositor glyph, while a
 * named group reference renders a variable-width octagon carrying its name.
 * Exactly one of {@code compositor} and {@code name} is set.
 */
public record GroupStyle(Compositor compositor, String name) {

    public GroupStyle {
        name = name == null ? "" : name;
        if ((compositor == null) == name.isEmpty()) {
            throw new IllegalArgumentException(
                    "A group style needs exactly one of a compositor or a non-empty name.");
        }
    }

    public static GroupStyle compositor(Compositor compositor) {
        return new GroupStyle(compositor, "");
    }

    public static GroupStyle named(String name) {
        return new GroupStyle(null, name);
    }

    public boolean isNamed() {
        return !name.isEmpty();
    }
}
