package org.jxsd.rendering;

/**
 * Text-measured node geometry shared by the page renderers: the width of an
 * element/attribute node (its measured bold label plus glyph room) and the
 * emitted height of a documentation panel.
 */
final class NodeGeometry {

    private static final float SIZE = 12f;
    private static final String WEIGHT = "600";
    private static final String STYLE = "normal";
    private static final int MIN_WIDTH = 45;
    private static final int LEAF_GLYPH_SPACE = 16;
    private static final int EXPAND_GLYPH_SPACE = 21;

    private NodeGeometry() {
    }

    /** Width of a node: the measured label background plus {@code glyphSpace}. */
    static int nodeWidth(String name, int glyphSpace) {
        float advance = SegoeUiMetrics.instance()
                .advanceWidth(name, SIZE, SegoeUiMetrics.FAMILY, WEIGHT, STYLE);
        return Math.max(MIN_WIDTH, (int) Math.ceil(advance) + 1 + glyphSpace);
    }

    /** Width of an element node, reserving room for the expand box when needed. */
    static int elementWidth(String name, boolean canExpand) {
        return nodeWidth(name, canExpand ? EXPAND_GLYPH_SPACE : LEAF_GLYPH_SPACE);
    }

    /** Height of a documentation panel at its 12px single-line box height. */
    static int emittedHeight(Documentation documentation) {
        return documentation.height(12);
    }
}
