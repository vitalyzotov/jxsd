package org.jxsd.rendering;

import java.io.Writer;
import java.util.List;
import org.jxsd.model.Compositor;

/**
 * Renders a reference-style page for a complex type that has both an
 * attribute tab and a content group: {@code complexType -> (attributes, group
 * -> element+)}. The type node sits between the attribute tab (above) and the
 * group (below).
 */
public final class CompositePage extends SvgPage {

    private static final int NODE_GAP = 13;

    private static final String EXPAND_BOX = """
            \t\t<rect x="%s" y="%s" width="11" height="11"/>
            \t\t<rect fill="white" x="%s" y="%s" width="9" height="9"/>
            \t\t<rect x="%s" y="%s" width="7" height="1"/>
            """;

    private final Connector connector;
    private final TypeNode typeNode;

    public CompositePage(Writer writer, RenderOptions options) {
        super(writer, options);
        this.connector = new Connector(writer);
        this.typeNode = new TypeNode(writer, options);
    }

    public void render(int pageNumber, String typeName, String typeDocumentation, Compositor groupType,
                       List<AttributeNode> attributes, List<ElementView> children) {
        int typeWidth = NodeGeometry.nodeWidth(typeName, 18);
        Documentation typeDoc = typeDocumentation == null ? null
                : Documentation.of(typeDocumentation);
        int typeDocumentationRight = typeDoc == null ? 0 : 5 + typeDoc.width();
        int groupX = (typeDoc == null ? typeWidth : typeDocumentationRight) + NODE_GAP;

        CompositeBody body = new CompositeBody(
                writer, options, groupX, 0, groupType, attributes, children);
        int connectorY = (body.containerCenter() + body.groupCenter()) / 2;
        int typeY = connectorY - 10;

        int canvasWidth = body.canvasWidth(typeDocumentationRight);
        int canvasHeight = body.canvasHeight();

        open(pageNumber, canvasWidth, canvasHeight);

        int branchX = body.branchX();
        int containerCenter = body.containerCenter();
        int groupCenter = body.groupCenter();
        connector.line(typeWidth + 4, connectorY, branchX - 1, connectorY);
        connector.line(branchX, connectorY, branchX, containerCenter + 1);
        connector.line(branchX, connectorY, branchX, groupCenter - 1);
        connector.line(branchX, groupCenter, groupX - 1, groupCenter);
        connector.line(branchX, containerCenter, groupX - 1, containerCenter);
        typeNode.render(0, typeY, typeName, typeDocumentation);

        body.draw();
        writeExpandBox(typeWidth - 6, typeY + 5);

        close();
    }

    private void writeExpandBox(int boxX, int boxY) {
        write(EXPAND_BOX.formatted(boxX, boxY, boxX + 1, boxY + 1, boxX + 2, boxY + 5));
    }

}
