package org.jxsd.rendering;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.jxsd.model.Compositor;
import org.jxsd.model.Occurrence;

/**
 * Renders a reference-style chain page whose content group contains a
 * nested group among its element children. The layout is recursive: a group
 * child stacks its own children in a further column and is centred on them.
 */
public final class NestedPage extends SvgPage {

    private static final int GROUP_WIDTH = 35;
    private static final int ROW_HEIGHT = 21;
    private static final int CANVAS_RIGHT_MARGIN = 24;
    private static final int DOC_RIGHT_MARGIN = 20;
    private static final int CONNECTOR_OFFSET = 4;
    private static final int BRANCH_FROM_GROUP = 54;
    private static final int CHILD_FROM_BRANCH = 15;
    private static final int CHILD_FROM_GROUP_SINGLE = 69;
    private static final int EXPAND_BOX_WIDTH = 11;
    private static final int HALF_ROW = ROW_HEIGHT / 2;
    private static final int CHILD_AFTER_GROUP_SINGLE = CHILD_FROM_GROUP_SINGLE - GROUP_WIDTH;
    private static final int BRANCH_AFTER_GROUP = BRANCH_FROM_GROUP - GROUP_WIDTH;
    private static final int ELEMENT_NODE_GAP = 15;
    private static final int CONTAINER_PAD = 14;
    private static final int TITLE_HEIGHT = 28;
    private static final int TITLE_INSET = 8;
    private static final int TITLE_BASELINE = 20;
    private static final int CONTAINER_BOTTOM_MARGIN = 10;
    private static final int ELEMENT_CONNECTOR_INSET = 5;
    private static final int CONTAINER_GROUP_INSET = 3;
    private static final String EXTENSION_SUFFIX = " (extension)";

    private static final String CONTAINER_RECT = """
            \t\t<rect fill="#FFFFC0" x="%s" width="%s" height="%s"/>
            """;
    private static final String CONTAINER_BORDER = """
            \t\t<rect fill="none" stroke="gray" stroke-width="1" stroke-linecap="square" stroke-miterlimit="4" transform="translate(0.5 0.5)" x="%s" width="%s" height="%s"/>
            """;
    private static final String CONTAINER_RECT_AT = """
            \t\t<rect fill="#FFFFC0" x="%s" y="%s" width="%s" height="%s"/>
            """;
    private static final String CONTAINER_BORDER_AT = """
            \t\t<rect fill="none" stroke="gray" stroke-width="1" stroke-linecap="square" stroke-miterlimit="4" transform="translate(0.5 0.5)" x="%s" y="%s" width="%s" height="%s"/>
            """;
    private static final String EXPAND_BOX = """
            \t\t<rect x="%s" y="%s" width="11" height="11"/>
            \t\t<rect fill="white" x="%s" y="%s" width="9" height="9"/>
            \t\t<rect x="%s" y="%s" width="7" height="1"/>
            """;

    private final Connector connector;
    private final TypeNode typeNode;
    private final GroupNode groupNode;
    private final ElementNode elementNode;

    /** A nested node: a leaf element, a group, or an expanded element. */
    public sealed interface Node permits Leaf, Group, Expanded {
    }

    /** A leaf element node wrapping the shared element view. */
    public record Leaf(ElementView element) implements Node {
    }

    public record Group(GroupStyle style, Occurrence occurrence,
                        String documentation, List<Node> children) implements Node {
        public Group(Compositor compositor, Occurrence occurrence, List<Node> children) {
            this(GroupStyle.compositor(compositor), occurrence, null, children);
        }
    }

    /**
     * An element whose children are shown: the element node, plus either a yellow
     * type container (named type) or a plain nested column (inline type) holding
     * the element's content group.
     */
    public record Expanded(ElementView element, String typeName,
                           List<AttributeNode> attributes, Group content) implements Node {
    }

    /** Absolute bounds of an expanded element's yellow type container. */
    private record Container(int x, int y, int bodyX, int attrY, int attrCenter, int bottom, int right) {
    }

    private record Placed(Node node, int x, int y, int center, int consumed,
                          List<Placed> children, int childX, int branchX, int width,
                          Container container) {
        Placed(Node node, int x, int y, int center, int consumed,
               List<Placed> children, int childX, int branchX, int width) {
            this(node, x, y, center, consumed, children, childX, branchX, width, null);
        }
    }

    public NestedPage(Writer writer, RenderOptions options) {
        super(writer, options);
        this.connector = new Connector(writer);
        this.typeNode = new TypeNode(writer, options);
        this.groupNode = new GroupNode(writer, options);
        this.elementNode = new ElementNode(writer, options);
    }

    public void render(int pageNumber, String typeName, String typeDocumentation, Group root) {
        int typeWidth = NodeGeometry.nodeWidth(typeName, 18);
        Documentation typeDoc = typeDocumentation == null ? null
                : Documentation.of(typeDocumentation);
        int typeDocumentationRight = typeDoc == null ? 0 : 5 + typeDoc.width();
        int groupX = groupPlacement(typeWidth, typeDocumentation, typeDocumentationRight);

        Placed placed = layoutGroup(root, groupX, 0);
        int groupY = placed.y();
        int groupCenter = placed.center();

        Bounds bounds = new Bounds();
        bounds.includeVisualRight(groupX + groupWidth(root));
        measure(placed, bounds);
        int canvasWidth = Math.max(bounds.visualRight + CANVAS_RIGHT_MARGIN,
                Math.max(bounds.documentationRight, typeDocumentationRight) + DOC_RIGHT_MARGIN);
        int canvasHeight = Math.max(bounds.height, typeDoc == null ? 0 : groupY + 27 + NodeGeometry.emittedHeight(typeDoc) + 10);

        open(pageNumber, canvasWidth, canvasHeight);

        connector.line(typeWidth + CONNECTOR_OFFSET, groupCenter, groupX - 1, groupCenter);
        typeNode.render(0, groupY, typeName, typeDocumentation);
        draw(placed);
        writeGroupExpandBoxes(placed);
        writeExpandBox(typeWidth - 6, groupY + 5);
        close();
    }

    /**
     * Renders a type root whose content group carries expanded children: the type
     * node, an optional attribute tab above the content group, and the recursive
     * group layout. Mirrors a composite page when {@code attributes} is not empty.
     */
    public void render(int pageNumber, String typeName, String typeDocumentation,
                       List<AttributeNode> attributes, Group root) {
        int typeWidth = NodeGeometry.nodeWidth(typeName, 18);
        Documentation typeDoc = typeDocumentation == null ? null
                : Documentation.of(typeDocumentation);
        int typeDocumentationRight = typeDoc == null ? 0 : 5 + typeDoc.width();
        int groupX = (typeDoc == null ? typeWidth : typeDocumentationRight) + 13;

        AttributeBox attributeBox = attributes.isEmpty() ? null
                : new AttributeBox(writer, options, groupX, 0, attributes);
        int contentTop = attributeBox == null ? 0 : attributeBox.bottom() + 3;
        Placed placed = layoutGroup(root, groupX, contentTop);
        int groupCenter = placed.center();
        int connectorY = attributeBox == null ? groupCenter : (attributeBox.center() + groupCenter) / 2;
        int typeY = connectorY - 10;

        Bounds bounds = new Bounds();
        bounds.includeVisualRight(groupX + groupWidth(root));
        measure(placed, bounds);
        int canvasWidth = Math.max(bounds.visualRight + CANVAS_RIGHT_MARGIN,
                Math.max(bounds.documentationRight, typeDocumentationRight) + DOC_RIGHT_MARGIN);
        if (attributeBox != null) {
            canvasWidth = Math.max(canvasWidth, attributeBox.right() + DOC_RIGHT_MARGIN);
        }
        int canvasHeight = Math.max(bounds.height, attributeBox == null ? 0 : attributeBox.bottom());
        if (typeDoc != null) {
            canvasHeight = Math.max(canvasHeight, typeY + 27 + NodeGeometry.emittedHeight(typeDoc) + 10);
        }

        open(pageNumber, canvasWidth, canvasHeight);
        if (attributeBox == null) {
            connector.line(typeWidth + CONNECTOR_OFFSET, groupCenter, groupX - 1, groupCenter);
        } else {
            int branchX = groupX - 7;
            connector.line(typeWidth + CONNECTOR_OFFSET, connectorY, branchX - 1, connectorY);
            connector.line(branchX, connectorY, branchX, attributeBox.center() + 1);
            connector.line(branchX, connectorY, branchX, groupCenter - 1);
            connector.line(branchX, attributeBox.center(), groupX - 1, attributeBox.center());
            connector.line(branchX, groupCenter, groupX - 1, groupCenter);
        }
        typeNode.render(0, typeY, typeName, typeDocumentation);
        if (attributeBox != null) {
            attributeBox.draw();
        }
        draw(placed);
        writeGroupExpandBoxes(placed);
        writeExpandBox(typeWidth - 6, typeY + 5);
        close();
    }

    /**
     * Renders an element whose type content contains a nested group: the element
     * node on the left, then a yellow container with the type title and the
     * nested group layout, matching the XMLSpy reference.
     */
    public void renderElementContext(int pageNumber, ElementView element, String typeName, Group root) {
        int elementWidth = NodeGeometry.elementWidth(element.name(), element.canExpand());
        Documentation elementDoc = element.documentation() == null ? null
                : Documentation.of(element.documentation());
        int elementDocRight = elementDoc == null ? 0 : 5 + elementDoc.width();
        int containerX = Math.max(elementWidth, elementDocRight) + ELEMENT_NODE_GAP;
        int bodyDx = containerX + CONTAINER_PAD;
        int bodyDy = TITLE_HEIGHT;
        int titleWidth = titleWidth(typeName);

        Placed placed = layoutGroup(root, bodyDx, bodyDy);
        Bounds bounds = new Bounds();
        bounds.includeVisualRight(bodyDx + groupWidth(root));
        measure(placed, bounds);
        int containerRight = Math.max(bounds.visualRight + CONTAINER_PAD,
                containerX + TITLE_INSET + titleWidth + CONTAINER_PAD);
        int containerBottom = bounds.height + CONTAINER_PAD;

        int elementY = placed.center() - HALF_ROW;
        int elementDocBottom = elementDoc == null ? 0 : elementY + 27 + NodeGeometry.emittedHeight(elementDoc);
        int elementBottom = Math.max(elementY + ROW_HEIGHT, elementDocBottom);
        int canvasWidth = Math.max(containerRight + CANVAS_RIGHT_MARGIN,
                Math.max(elementWidth, elementDocRight) + CANVAS_RIGHT_MARGIN);
        int canvasHeight = Math.max(containerBottom, elementBottom) + CONTAINER_BOTTOM_MARGIN;

        open(pageNumber, canvasWidth, canvasHeight);
        write(CONTAINER_RECT.formatted(containerX, containerRight - containerX, containerBottom));
        write(CONTAINER_BORDER.formatted(containerX, containerRight - containerX - 1, containerBottom - 1));
        if (!typeName.isEmpty()) {
            Text.write(writer, options, containerX + TITLE_INSET, TITLE_BASELINE,
                    typeName, 12f, "600", null, "gray");
        }
        connector.line(elementWidth - ELEMENT_CONNECTOR_INSET, placed.center(), bodyDx - 1, placed.center());
        elementNode.render(0, elementY, element.name(), element.documentation(), element.canExpand(), true,
                element.showsText(), element.occurrence());
        draw(placed);
        writeGroupExpandBoxes(placed);
        writeExpandBox(elementWidth - 6, elementY + 5);
        close();
    }

    /**
     * Like {@link #renderElementContext} but for an element root whose type adds
     * an attribute tab inside the container (an expanded element context).
     */
    public void renderElementContext(int pageNumber, ElementView element, String typeName,
                                     List<AttributeNode> attributes, Group root) {
        int elementWidth = NodeGeometry.elementWidth(element.name(), element.canExpand());
        Documentation elementDoc = element.documentation() == null ? null
                : Documentation.of(element.documentation());
        int elementDocRight = elementDoc == null ? 0 : 5 + elementDoc.width();
        int containerX = Math.max(elementWidth, elementDocRight) + ELEMENT_NODE_GAP;
        int bodyDx = containerX + CONTAINER_PAD;
        int titleWidth = titleWidth(typeName);

        AttributeBox attributeBox = attributes.isEmpty() ? null
                : new AttributeBox(writer, options, bodyDx, TITLE_HEIGHT, attributes);
        int contentTop = attributeBox == null ? TITLE_HEIGHT : attributeBox.bottom() + CONTAINER_GROUP_INSET;
        Placed placed = layoutGroup(root, bodyDx, contentTop);

        Bounds bounds = new Bounds();
        bounds.includeVisualRight(bodyDx + groupWidth(root));
        measure(placed, bounds);
        int contentRight = bounds.visualRight;
        int contentBottom = bounds.height;
        if (attributeBox != null) {
            contentRight = Math.max(contentRight, attributeBox.right());
            contentBottom = Math.max(contentBottom, attributeBox.bottom());
        }
        int containerRight = Math.max(contentRight + CONTAINER_PAD,
                containerX + TITLE_INSET + titleWidth + CONTAINER_PAD);
        int containerBottom = contentBottom + CONTAINER_PAD;

        int elementY = placed.center() - HALF_ROW;
        int elementDocBottom = elementDoc == null ? 0 : elementY + 27 + NodeGeometry.emittedHeight(elementDoc);
        int elementBottom = Math.max(elementY + ROW_HEIGHT, elementDocBottom);
        int canvasWidth = Math.max(containerRight + CANVAS_RIGHT_MARGIN,
                Math.max(elementWidth, elementDocRight) + CANVAS_RIGHT_MARGIN);
        int canvasHeight = Math.max(containerBottom, elementBottom) + CONTAINER_BOTTOM_MARGIN;

        open(pageNumber, canvasWidth, canvasHeight);
        write(CONTAINER_RECT.formatted(containerX, containerRight - containerX, containerBottom));
        write(CONTAINER_BORDER.formatted(containerX, containerRight - containerX - 1, containerBottom - 1));
        if (!typeName.isEmpty()) {
            Text.write(writer, options, containerX + TITLE_INSET, TITLE_BASELINE,
                    typeName, 12f, "600", null, "gray");
        }
        int elementRight = elementWidth - ELEMENT_CONNECTOR_INSET;
        if (attributeBox == null) {
            connector.line(elementRight, placed.center(), bodyDx - 1, placed.center());
        } else {
            int branchX = containerX + CONTAINER_GROUP_INSET;
            connector.line(elementRight, placed.center(), branchX - 1, placed.center());
            connector.line(branchX, placed.center(), bodyDx - 1, placed.center());
            connector.line(branchX, placed.center(), branchX, attributeBox.center());
            connector.line(branchX, attributeBox.center(), bodyDx - 1, attributeBox.center());
        }
        elementNode.render(0, elementY, element.name(), element.documentation(), element.canExpand(), true,
                element.showsText(), element.occurrence());
        if (attributeBox != null) {
            attributeBox.draw();
        }
        draw(placed);
        writeGroupExpandBoxes(placed);
        writeExpandBox(elementWidth - 6, elementY + 5);
        close();
    }

    /**
     * Renders an {@code xs:extension} type root whose base and derived content
     * carry expanded children: the derived type node, the yellow base container
     * (attributes and recursive base content) and the recursive derived content
     * below it.
     */
    public void renderExtension(int pageNumber, String derivedName, String derivedDocumentation,
                                String baseLabel, List<AttributeNode> baseAttributes,
                                Group baseContent, List<AttributeNode> derivedAttributes,
                                Group derivedContent) {
        int derivedWidth = NodeGeometry.nodeWidth(derivedName, 18);
        Documentation derivedDoc = derivedDocumentation == null ? null : Documentation.of(derivedDocumentation);
        int derivedDocRight = derivedDoc == null ? 0 : 5 + derivedDoc.width();
        int containerX = Math.max(derivedWidth, derivedDocRight) + 13;
        int bodyDx = containerX + CONTAINER_PAD;
        int titleWidth = titleWidth(baseLabel + EXTENSION_SUFFIX);

        AttributeBox baseAttr = baseAttributes.isEmpty() ? null
                : new AttributeBox(writer, options, bodyDx, TITLE_HEIGHT, baseAttributes);
        int baseTop = baseAttr == null ? TITLE_HEIGHT : baseAttr.bottom() + CONTAINER_GROUP_INSET;
        Placed base = baseContent == null ? null : layoutGroup(baseContent, bodyDx, baseTop);
        int baseRight = base != null ? visualRight(base) : (baseAttr != null ? baseAttr.right() : bodyDx);
        int baseBottom = base != null
                ? Math.max(base.y() + base.consumed(), baseAttr == null ? 0 : baseAttr.bottom())
                : (baseAttr == null ? TITLE_HEIGHT : baseAttr.bottom());
        int containerRight = Math.max(baseRight + CONTAINER_PAD,
                containerX + TITLE_INSET + titleWidth + CONTAINER_PAD);
        int containerBottom = baseBottom + CONTAINER_PAD;

        AttributeBox derivedAttr = derivedAttributes.isEmpty() || derivedContent == null ? null
                : new AttributeBox(writer, options, bodyDx, containerBottom + 24, derivedAttributes);
        int derivedTop = derivedAttr == null ? containerBottom + 24 : derivedAttr.bottom() + 24;
        Placed derived = derivedContent == null ? null : layoutGroup(derivedContent, bodyDx, derivedTop);

        int branchX = bodyDx - 7;
        int baseCenter = base != null
                ? base.center()
                : (baseAttr != null ? baseAttr.center() : TITLE_HEIGHT + ROW_HEIGHT / 2);
        int connectorY = baseAttr == null ? baseCenter : (baseAttr.center() + baseCenter) / 2;
        int spineY = base != null ? baseCenter : connectorY;
        int typeY = connectorY - 10;

        Bounds bounds = new Bounds();
        bounds.includeVisualRight(containerRight);
        bounds.includeHeight(containerBottom);
        if (base != null) {
            measure(base, bounds);
        }
        if (derived != null) {
            measure(derived, bounds);
        }
        if (derivedAttr != null) {
            bounds.includeVisualRight(derivedAttr.right());
            bounds.includeHeight(derivedAttr.bottom());
        }
        int canvasWidth = Math.max(bounds.visualRight + CANVAS_RIGHT_MARGIN,
                Math.max(bounds.documentationRight, derivedDocRight) + DOC_RIGHT_MARGIN);
        int canvasHeight = Math.max(bounds.height, containerBottom);
        if (derivedDoc != null) {
            canvasHeight = Math.max(canvasHeight, typeY + 27 + NodeGeometry.emittedHeight(derivedDoc) + 10);
        }

        open(pageNumber, canvasWidth, canvasHeight);
        write(CONTAINER_RECT_AT.formatted(containerX, 0, containerRight - containerX, containerBottom));
        write(CONTAINER_BORDER_AT.formatted(containerX, 0, containerRight - containerX - 1, containerBottom - 1));
        float baseLabelWidth = SegoeUiMetrics.instance()
                .advanceWidth(baseLabel, 12f, SegoeUiMetrics.FAMILY, "600", "normal");
        Text.write(writer, options, containerX + TITLE_INSET, TITLE_BASELINE, baseLabel, 12f, "600", null, "gray");
        Text.write(writer, options, containerX + TITLE_INSET + baseLabelWidth, TITLE_BASELINE,
                EXTENSION_SUFFIX, 12f, null, null, "gray");

        connector.line(derivedWidth + CONNECTOR_OFFSET, connectorY, branchX - 1, connectorY);
        if (baseAttr != null) {
            if (base != null) {
                connector.line(branchX, connectorY, branchX, baseAttr.center() + 1);
            }
            connector.line(branchX, baseAttr.center(), bodyDx - 1, baseAttr.center());
        }
        if (base != null) {
            connector.line(branchX, connectorY, branchX, baseCenter - 1);
            connector.line(branchX, baseCenter, bodyDx - 1, baseCenter);
        }
        if (derivedAttr != null) {
            connector.line(branchX, spineY, branchX, derivedAttr.center());
            connector.line(branchX, derivedAttr.center(), bodyDx - 1, derivedAttr.center());
        }
        if (derived != null) {
            connector.line(branchX, spineY, branchX, derived.center());
            connector.line(branchX, derived.center(), bodyDx - 1, derived.center());
        }

        if (baseAttr != null) {
            baseAttr.draw();
        }
        if (base != null) {
            draw(base);
        }
        if (derivedAttr != null) {
            derivedAttr.draw();
        }
        if (derived != null) {
            draw(derived);
            writeGroupExpandBoxes(derived);
        }
        if (base != null) {
            writeGroupExpandBoxes(base);
        }

        typeNode.render(0, typeY, derivedName, derivedDocumentation);
        writeExpandBox(derivedWidth - 6, typeY + 5);
        close();
    }

    private static int titleWidth(String name) {
        if (name == null || name.isEmpty()) {
            return 0;
        }
        float advance = SegoeUiMetrics.instance()
                .advanceWidth(name, 12f, SegoeUiMetrics.FAMILY, "600", "normal");
        return (int) Math.ceil(advance) + 1;
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    private Placed layoutGroup(Group group, int x, int columnTop) {
        int width = groupWidth(group);
        boolean single = group.children().size() == 1;
        int branchX = x + width + BRANCH_AFTER_GROUP;
        if (group.documentation() != null) {
            Documentation doc = Documentation.of(group.documentation());
            branchX = Math.max(branchX, x + 5 + doc.width() + 6);
        }
        int childX = single ? x + width + CHILD_AFTER_GROUP_SINGLE : branchX + CHILD_FROM_BRANCH;
        List<Placed> children = new ArrayList<>();
        List<Node> nodes = group.children();
        int cursor = columnTop;
        for (Node child : nodes) {
            if (child instanceof Leaf leaf) {
                ElementView element = leaf.element();
                int consumed = element.consumed();
                children.add(new Placed(leaf, childX, cursor, cursor + HALF_ROW, consumed, List.of(), 0, 0,
                        NodeGeometry.elementWidth(element.name(), element.canExpand())));
                cursor += consumed + Metrics.SIBLING_ELEMENT_GAP;
            } else if (child instanceof Group nested) {
                Placed placed = layoutGroup(nested, childX, cursor);
                children.add(new Placed(nested, childX, placed.y(), placed.center(), placed.consumed(),
                        placed.children(), placed.childX(), placed.branchX(), groupWidth(nested)));
                cursor += placed.consumed() + Metrics.SIBLING_ELEMENT_GAP;
            } else if (child instanceof Expanded expanded) {
                Placed placed = layoutExpanded(expanded, childX, cursor);
                children.add(placed);
                cursor += placed.consumed() + Metrics.SIBLING_ELEMENT_GAP;
            }
        }
        int nodeY = children.isEmpty() ? columnTop
                : (children.getFirst().center() + children.getLast().center()) / 2 - HALF_ROW;
        int columnHeight = children.isEmpty() ? ROW_HEIGHT
                : cursor - Metrics.SIBLING_ELEMENT_GAP - columnTop;
        return new Placed(group, x, nodeY, nodeY + HALF_ROW, columnHeight, children, childX, branchX, width);
    }

    /**
     * Lays out an expanded element: the element node in the current column, then
     * either a yellow type container (named type) or a plain nested column
     * (inline type) holding the element's content group.
     */
    private Placed layoutExpanded(Expanded expanded, int x, int columnTop) {
        int elementWidth = NodeGeometry.elementWidth(expanded.element().name(), expanded.element().canExpand());
        boolean named = expanded.typeName() != null && !expanded.typeName().isEmpty();
        int containerX = x + elementWidth + ELEMENT_NODE_GAP;
        int bodyX = named ? containerX + CONTAINER_PAD : containerX;
        int attrY = columnTop + TITLE_HEIGHT;
        AttributeBox attributeBox = expanded.attributes().isEmpty() ? null
                : new AttributeBox(new java.io.StringWriter(), options, bodyX, attrY, expanded.attributes());
        int contentTop = attributeBox == null ? (named ? columnTop + TITLE_HEIGHT : columnTop)
                : attributeBox.bottom() + CONTAINER_GROUP_INSET;
        Placed content = layoutGroup(expanded.content(), bodyX, contentTop);
        int bodyBottom = Math.max(content.y() + content.consumed(),
                attributeBox == null ? 0 : attributeBox.bottom());
        int elementCenter;
        if (named && attributeBox != null) {
            elementCenter = (attributeBox.top() + bodyBottom) / 2;
        } else {
            elementCenter = content.center();
        }
        int elementY = elementCenter - HALF_ROW;
        int bottom = named
                ? Math.max(bodyBottom, elementY + ROW_HEIGHT) + CONTAINER_BOTTOM_MARGIN
                : Math.max(elementY + ROW_HEIGHT, content.y() + content.consumed());
        int consumed = bottom - columnTop;
        Container container = null;
        if (named) {
            int contentRight = visualRight(content);
            int containerRight = Math.max(contentRight + CONTAINER_PAD,
                    containerX + TITLE_INSET + titleWidth(expanded.typeName()) + CONTAINER_PAD);
            container = new Container(containerX, columnTop, bodyX, attrY,
                    attributeBox == null ? 0 : attributeBox.center(), bottom, containerRight);
        }
        return new Placed(expanded, x, elementY, elementCenter, consumed, List.of(content), 0, 0,
                elementWidth, container);
    }

    /** Rightmost drawn edge of a laid-out subtree (ignores documentation). */
    private static int visualRight(Placed placed) {
        if (placed.node() instanceof Leaf leaf) {
            return placed.x() + leaf.element().visualRight();
        }
        if (placed.node() instanceof Expanded && placed.container() != null) {
            return placed.container().right();
        }
        int right = placed.x() + placed.width();
        for (Placed child : placed.children()) {
            right = Math.max(right, visualRight(child));
        }
        return right;
    }

    /** A named group reference gets a width that fits its name; compositors stay 35px. */
    private static int groupWidth(Group group) {
        if (!group.style().isNamed()) {
            return GROUP_WIDTH;
        }
        float advance = SegoeUiMetrics.instance()
                .advanceWidth(group.style().name(), 12f, SegoeUiMetrics.FAMILY, "400", "normal");
        return Math.max(45, (int) Math.ceil(advance) + 10);
    }

    private static final class Bounds {
        private int visualRight;
        private int documentationRight;
        private int height;

        void includeVisualRight(int x) {
            visualRight = Math.max(visualRight, x);
        }

        void includeDocumentationRight(int x) {
            documentationRight = Math.max(documentationRight, x);
        }

        void includeHeight(int y) {
            height = Math.max(height, y);
        }
    }

    private void measure(Placed placed, Bounds bounds) {
        if (placed.node() instanceof Leaf leaf) {
            ElementView element = leaf.element();
            bounds.includeVisualRight(placed.x() + element.visualRight());
            if (element.documentation() != null) {
                bounds.includeDocumentationRight(placed.x() + element.documentationRight());
            }
            bounds.includeHeight(placed.y() + placed.consumed() + element.bottomMargin());
        } else if (placed.node() instanceof Group group) {
            bounds.includeVisualRight(placed.x() + placed.width());
            bounds.includeHeight(placed.y() + ROW_HEIGHT + 14);
            if (group.documentation() != null) {
                Documentation doc = Documentation.of(group.documentation());
                bounds.includeDocumentationRight(placed.x() + 5 + doc.width());
                bounds.includeHeight(placed.y() + 27 + doc.height(12));
            }
            if (group.occurrence().isRepeated()) {
                bounds.includeHeight(placed.y() + 43 + 10);
            }
        } else if (placed.node() instanceof Expanded expanded) {
            if (placed.container() != null) {
                bounds.includeVisualRight(placed.container().right());
                bounds.includeHeight(placed.container().bottom());
            } else {
                bounds.includeVisualRight(placed.x() + placed.width());
                bounds.includeHeight(placed.y() + placed.consumed() + 14);
            }
            if (expanded.element().documentation() != null) {
                Documentation doc = Documentation.of(expanded.element().documentation());
                bounds.includeDocumentationRight(placed.x() + 5 + doc.width());
                bounds.includeHeight(placed.y() + 27 + doc.height(12));
            }
        }
        for (Placed child : placed.children()) {
            measure(child, bounds);
        }
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    private void draw(Placed placed) {
        if (placed.node() instanceof Leaf leaf) {
            ElementView element = leaf.element();
            elementNode.render(placed.x(), placed.y(), element.name(), element.documentation(),
                    element.canExpand(), element.expanded(), element.showsText(), element.occurrence());
            return;
        }
        if (placed.node() instanceof Expanded expanded) {
            drawExpanded(placed, expanded);
            return;
        }
        Group group = (Group) placed.node();
        writeConnectors(placed);
        if (isNamedGroup(group)) {
            groupNode.renderNamed(placed.x(), placed.y(), placed.width(), group.style().name(),
                    group.occurrence(), group.documentation());
        } else {
            groupNode.render(placed.x(), placed.y(), group.style().compositor(), group.occurrence(),
                    group.documentation());
        }
        for (Placed child : placed.children()) {
            draw(child);
        }
    }

    private void drawExpanded(Placed placed, Expanded expanded) {
        Placed content = placed.children().getFirst();
        Container container = placed.container();
        if (container != null) {
            write(CONTAINER_RECT_AT.formatted(container.x(), container.y(),
                    container.right() - container.x(), container.bottom() - container.y()));
            write(CONTAINER_BORDER_AT.formatted(container.x(), container.y(),
                    container.right() - container.x() - 1, container.bottom() - container.y() - 1));
            Text.write(writer, options, container.x() + TITLE_INSET, container.y() + TITLE_BASELINE,
                    expanded.typeName(), 12f, "600", null, "gray");
            int elementRight = placed.x() + placed.width() - ELEMENT_CONNECTOR_INSET;
            if (expanded.attributes().isEmpty()) {
                connector.line(elementRight, placed.center(), container.bodyX() - 1, placed.center());
            } else {
                new AttributeBox(writer, options, container.bodyX(), container.attrY(),
                        expanded.attributes()).draw();
                int branchX = container.x() + CONTAINER_GROUP_INSET;
                connector.line(elementRight, placed.center(), branchX - 1, placed.center());
                connector.line(branchX, placed.center(), branchX, container.attrCenter());
                connector.line(branchX, container.attrCenter(), container.bodyX() - 1, container.attrCenter());
                connector.line(branchX, placed.center(), branchX, content.center());
                connector.line(branchX, content.center(), container.bodyX() - 1, content.center());
            }
        } else {
            connector.line(placed.x() + placed.width() - ELEMENT_CONNECTOR_INSET, placed.center(),
                    content.x() - 1, placed.center());
        }
        ElementView element = expanded.element();
        elementNode.render(placed.x(), placed.y(), element.name(), element.documentation(),
                element.canExpand(), element.expanded(), element.showsText(), element.occurrence());
        draw(content);
    }

    private static boolean isNamedGroup(Group group) {
        return group.style().isNamed();
    }

    private void writeGroupExpandBoxes(Placed placed) {
        for (Placed child : placed.children()) {
            writeGroupExpandBoxes(child);
        }
        if (placed.node() instanceof Group group && !isNamedGroup(group)) {
            writeExpandBox(placed.x() + placed.width() - 1, placed.y() + 5);
        }
    }

    private void writeConnectors(Placed group) {
        List<Placed> children = group.children();
        boolean single = children.size() == 1;
        int groupX = group.x();
        int groupCenter = group.center();
        int childX = group.childX();
        int groupEdge = groupX + group.width() - 1
                + (group.node() instanceof Group node && isNamedGroup(node) ? 0 : EXPAND_BOX_WIDTH);
        if (single) {
            connector.line(groupEdge, groupCenter, childX - 1, groupCenter);
            return;
        }
        int branchX = group.branchX();
        List<ChildConnectors.Child> childCenters = new ArrayList<>();
        for (Placed child : children) {
            childCenters.add(new ChildConnectors.Child(minOccurs(child.node()), child.center()));
        }
        ChildConnectors.drawBranch(connector, groupEdge, groupCenter, branchX, childX, childCenters);
    }

    private static int minOccurs(Node node) {
        if (node instanceof Leaf leaf) {
            return leaf.element().occurrence().min();
        }
        if (node instanceof Expanded expanded) {
            return expanded.element().occurrence().min();
        }
        return ((Group) node).occurrence().min();
    }

    private static int groupPlacement(int typeWidth, String typeDocumentation, int typeDocumentationRight) {
        if (typeDocumentation == null) {
            return typeWidth + 6;
        }
        if (typeDocumentationRight <= typeWidth) {
            return typeWidth + 10;
        }
        return typeDocumentationRight + 7;
    }

    private void writeExpandBox(int boxX, int boxY) {
        write(EXPAND_BOX.formatted(boxX, boxY, boxX + 1, boxY + 1, boxX + 2, boxY + 5));
    }
}
