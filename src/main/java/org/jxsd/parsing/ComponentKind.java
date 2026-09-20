package org.jxsd.parsing;

/** The top-level XSD schema component kind. */
public enum ComponentKind {
    ELEMENT("element"),
    COMPLEX_TYPE("complexType"),
    SIMPLE_TYPE("simpleType"),
    GROUP("group"),
    ATTRIBUTE("attribute"),
    ATTRIBUTE_GROUP("attributeGroup");

    private final String displayName;

    ComponentKind(String displayName) {
        this.displayName = displayName;
    }

    /** The spelling used on the command line and in messages. */
    public String displayName() {
        return displayName;
    }

    /** Resolves a display name, or {@code null} when it names no kind. */
    public static ComponentKind fromDisplayName(String name) {
        for (ComponentKind kind : values()) {
            if (kind.displayName.equals(name)) {
                return kind;
            }
        }
        return null;
    }
}
