package org.jxsd.rendering;

import java.io.Writer;
import java.util.List;

/**
 * The notched attribute tab used by composite bodies and extension containers:
 * an italic {@code attributes} heading with the attribute rows and their
 * documentation. Geometry is computed from the top-left corner, so the box can
 * be drawn anywhere.
 */
final class AttributeBox extends SvgPage {

    private static final int ROW_HEIGHT = 21;
    private static final int TAB_HEIGHT = 19;
    private static final int HEADING_INSET = 20;
    private static final int HEADING_PAD = 6;
    private static final int ATTRIBUTE_INSET = 6;
    private static final int ATTRIBUTE_TOP = 23;
    private static final String HEADING = "attributes";

    private static final String FILL_PATH = """
            \t\t<path fill="white" d="%s"/>
            """;
    private static final String OUTLINE_PATH = """
            \t\t<path %s d="%s"/>
            """;
    private static final String WHITE_RECT = """
            \t\t<rect fill="white" x="%s" y="%s" width="%s" height="16"/>
            """;
    private static final String EXPAND_BOX = """
            \t\t<rect x="%s" y="%s" width="11" height="11"/>
            \t\t<rect fill="white" x="%s" y="%s" width="9" height="9"/>
            \t\t<rect x="%s" y="%s" width="7" height="1"/>
            """;
    private static final String POLYGON = "M%s %sL%s %sL%s %sL%s %sL%s %sL%s %sL%s %sZ";
    private static final String NOTCHED_POLYGON =
            "M%s %sL%s %sL%s %sL%s %sL%s %sL%s %sL%s %sL%s %sZ";

    private final SegoeUiMetrics metrics = SegoeUiMetrics.instance();
    private final ElementNode elementNode;
    private final int x;
    private final int y;
    private final List<AttributeNode> attributes;
    private final int right;
    private final int bottom;

    AttributeBox(Writer writer, RenderOptions options, int x, int y,
                          List<AttributeNode> attributes) {
        super(writer, options);
        this.elementNode = new ElementNode(writer, options);
        this.x = x;
        this.y = y;
        this.attributes = attributes;

        int headingRight = x + HEADING_INSET + headingWidth() + HEADING_PAD;
        int maxAttributeBottom = y + ATTRIBUTE_TOP + ROW_HEIGHT;
        int maxDocumentationBottom = 0;
        int maxAttributeRight = x + ATTRIBUTE_INSET;
        int maxDocumentationRight = 0;
        int cursor = y + ATTRIBUTE_TOP;
        for (AttributeNode attribute : attributes) {
            Documentation doc = docOf(attribute);
            maxAttributeBottom = Math.max(maxAttributeBottom, cursor + ROW_HEIGHT);
            if (doc != null) {
                maxDocumentationBottom = Math.max(maxDocumentationBottom, cursor + 27 + doc.height(12));
            }
            maxAttributeRight = Math.max(maxAttributeRight, x + ATTRIBUTE_INSET + NodeGeometry.nodeWidth(attribute.name(), 16));
            if (doc != null) {
                maxDocumentationRight = Math.max(maxDocumentationRight, x + ATTRIBUTE_INSET + 5 + doc.width());
            }
            cursor += attributeAdvance(doc);
        }
        this.bottom = Math.max(maxAttributeBottom + 5, maxDocumentationBottom + 1);
        this.right = Math.max(headingRight, Math.max(maxAttributeRight + 8, maxDocumentationRight + 3));
    }

    int top() {
        return y;
    }

    int right() {
        return right;
    }

    int bottom() {
        return bottom;
    }

    int center() {
        return (y + bottom) / 2;
    }

    void draw() {
        int headingX = x + HEADING_INSET;
        int tabRight = headingX + headingWidth() + HEADING_PAD;
        String polygon = right != tabRight
                ? NOTCHED_POLYGON.formatted(tabRight, y + TAB_HEIGHT, tabRight, y, x, y, x, y + TAB_HEIGHT,
                        x, bottom, right, bottom, right, y + TAB_HEIGHT, tabRight, y + TAB_HEIGHT)
                : POLYGON.formatted(tabRight, y + TAB_HEIGHT, tabRight, y, x, y, x, y + TAB_HEIGHT,
                        x, bottom, right, bottom, right, y + TAB_HEIGHT);
        write(FILL_PATH.formatted(polygon));
        write(WHITE_RECT.formatted(headingX, y + 2, headingWidth()));
        Text.write(writer, options, headingX, y + 15, HEADING, 12f, "400", "italic", null);
        write(OUTLINE_PATH.formatted(Shapes.SOLID_STROKE, polygon));
        int cursor = y + ATTRIBUTE_TOP;
        for (AttributeNode attribute : attributes) {
            elementNode.renderAttribute(x + ATTRIBUTE_INSET, cursor, attribute.name(), attribute.documentation(),
                    false);
            cursor += attributeAdvance(docOf(attribute));
        }
        writeExpandBox(x + 5, y + 5);
    }

    private void writeExpandBox(int boxX, int boxY) {
        write(EXPAND_BOX.formatted(boxX, boxY, boxX + 1, boxY + 1, boxX + 2, boxY + 5));
    }

    private static Documentation docOf(AttributeNode attribute) {
        return attribute.documentation() == null ? null : Documentation.of(attribute.documentation());
    }

    private static int attributeAdvance(Documentation doc) {
        int base = doc == null ? ROW_HEIGHT : 27 + doc.height(12);
        return base + Metrics.SIBLING_ATTRIBUTE_GAP;
    }

    private int headingWidth() {
        float advance = metrics.advanceWidth(HEADING, 12f, SegoeUiMetrics.FAMILY, "400", "italic");
        return (int) Math.ceil(advance + 1f);
    }

}
