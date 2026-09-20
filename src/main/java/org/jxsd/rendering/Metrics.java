package org.jxsd.rendering;

/**
 * Geometry recovered from the XMLSpy reference diagrams, kept in one place so
 * the renderer and its tests share the same constants. The corpus is
 * {@code tools/xmlspy/xmlspy.conformance.v1.xsd}; regenerate the XMLSpy side
 * there to re-derive any value.
 */
public final class Metrics {

    /** Vertical gap between stacked element nodes (XMLSpy pitch 33, height 21). */
    public static final int SIBLING_ELEMENT_GAP = 12;

    /** Vertical gap between stacked attribute nodes (XMLSpy pitch 28, height 21). */
    public static final int SIBLING_ATTRIBUTE_GAP = 7;

    /** Labels wider than this are truncated and suffixed with an ellipsis. */
    public static final int LABEL_TRUNCATION_WIDTH = 170;

    /** Documentation wraps onto a new line before exceeding this width. */
    public static final int DOCUMENTATION_WRAP_WIDTH = 115;

    private Metrics() {
    }
}
