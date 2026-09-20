package org.jxsd.model;

/**
 * The XSD content kind of an element or complex type definition; a model group
 * or group reference is classified as {@link #ELEMENT_ONLY}.
 * {@code empty}, {@code simple}, {@code element-only} or {@code mixed}.
 */
public enum ContentType {
    EMPTY,
    SIMPLE,
    ELEMENT_ONLY,
    MIXED;

    /** True when the content holds a particle (element-only or mixed content). */
    public boolean hasParticle() {
        return this == ELEMENT_ONLY || this == MIXED;
    }

    /** True when character data is allowed (simple or mixed content). */
    public boolean allowsText() {
        return this == SIMPLE || this == MIXED;
    }

    /** Null-safe {@link #hasParticle()}: a node without content cannot expand. */
    public static boolean canExpand(ContentType type) {
        return type != null && type.hasParticle();
    }

    /** Null-safe {@link #allowsText()}: a node without content shows no text glyph. */
    public static boolean showsText(ContentType type) {
        return type != null && type.allowsText();
    }

    public static ContentType of(boolean hasParticle, boolean allowsText) {
        if (hasParticle) {
            return allowsText ? MIXED : ELEMENT_ONLY;
        }
        return allowsText ? SIMPLE : EMPTY;
    }
}
