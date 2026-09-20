package org.jxsd.model;

/**
 * The narrow view of a schema component the renderer is allowed to see: the
 * component's raw documentation. Keeps the Apache XmlSchema object model out of
 * the rendering code; the expander still reads the parsed object through the
 * package-private {@link DiagramNode#parsed()}.
 */
@FunctionalInterface
public interface NodeSource {

    /** A component without an annotation. */
    NodeSource NONE = language -> null;

    /** The component's raw documentation for {@code language}, or null when absent. */
    String rawDocumentation(String language);
}
