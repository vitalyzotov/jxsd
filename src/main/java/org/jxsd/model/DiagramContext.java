package org.jxsd.model;

import org.jxsd.parsing.Schema;

/**
 * Immutable snapshot of the schema registry and display options a built tree is
 * rendered with. Passed explicitly to the factory, the expander and the page
 * renderer so the model nodes no longer need a back-reference to the diagram.
 */
public record DiagramContext(Schema schema, boolean showDocumentation, String language) {

    /** A context without schema or documentation, used for null-root and unit checks. */
    public static final DiagramContext EMPTY = new DiagramContext(null, false, null);
}
