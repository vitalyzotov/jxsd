package org.jxsd.rendering;

import java.io.Writer;
import java.util.List;

/**
 * Renders a complex type that has no content: a single type hexagon with its
 * documentation, and (for a simple-content type) the {@code attributes} tab,
 * matching the XMLSpy reference.
 */
public final class TypePage extends SvgPage {

    private static final int TYPE_HEIGHT = 22;
    private static final int DOC_X = 5;
    private static final int DOC_BASELINE = 37;
    private static final int ATTRIBUTE_GAP = 18;
    private static final int CONNECTOR_INSET = 6;
    private static final int CONNECTOR_END_INSET = 11;
    private static final int CANVAS_RIGHT_MARGIN = 20;
    private static final int CANVAS_BOTTOM_MARGIN = 7;

    private final Connector connector;

    public TypePage(Writer writer, RenderOptions options) {
        super(writer, options);
        this.connector = new Connector(writer);
    }

    public void render(int pageNumber, String name, String rawDocumentation,
                       List<AttributeNode> attributes) {
        Documentation documentation = rawDocumentation == null || rawDocumentation.isEmpty()
                ? null : Documentation.of(rawDocumentation);
        int typeWidth = TypeNode.widthFor(name);
        int docWidth = documentation == null ? 0 : DOC_X + documentation.width();

        AttributeBox attributeBox = attributes.isEmpty() ? null
                : new AttributeBox(writer, options, typeWidth + ATTRIBUTE_GAP, 0, attributes);
        int typeTop = attributeBox == null ? 0 : Math.max(0, attributeBox.center() - TYPE_HEIGHT / 2);
        int docBottom = documentation == null ? typeTop + TYPE_HEIGHT
                : typeTop + DOC_BASELINE + (documentation.lines().size() - 1) * Documentation.LINE_HEIGHT;

        int canvasWidth = Math.max(typeWidth, docWidth) + CANVAS_RIGHT_MARGIN;
        int canvasHeight = Math.max(typeTop + TYPE_HEIGHT, docBottom) + CANVAS_BOTTOM_MARGIN;
        if (attributeBox != null) {
            canvasWidth = Math.max(canvasWidth, attributeBox.right() + CANVAS_RIGHT_MARGIN);
            canvasHeight = Math.max(canvasHeight, attributeBox.bottom() + CANVAS_BOTTOM_MARGIN);
        }
        open(pageNumber, canvasWidth, canvasHeight);
        if (attributeBox != null) {
            int center = typeTop + TYPE_HEIGHT / 2;
            connector.line(typeWidth - CONNECTOR_INSET, center, typeWidth + ATTRIBUTE_GAP - CONNECTOR_END_INSET, center);
        }
        new TypeNode(writer, options).render(0, typeTop, name, rawDocumentation);
        if (attributeBox != null) {
            attributeBox.draw();
        }
        close();
    }
}
