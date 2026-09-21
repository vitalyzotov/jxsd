package org.jxsd.rendering;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders an {@code xs:extension} page: the derived type node on the left, the
 * base type shown as a yellow container with a solid gray border, labelled
 * {@code Base (extension)} in gray, holding the inherited attributes and
 * content, and the derived type's own content group below the container.
 */
public final class ExtensionPage extends SvgPage {

    private static final int GROUP_WIDTH = 35;
    private static final int ROW_HEIGHT = 21;
    private static final int CHILD_FROM_GROUP = 69;
    private static final int BRANCH_FROM_GROUP = 54;
    private static final int EXPAND_BOX_WIDTH = 11;
    private static final int NODE_GAP = 13;
    private static final String EXTENSION_SUFFIX = " (extension)";
    private static final int CONTAINER_PAD = 14;
    private static final int TITLE_HEIGHT = 28;
    private static final int TITLE_BASELINE = 20;
    private static final int TITLE_INSET = 8;
    private static final int DERIVED_GAP = 24;
    private static final int CONNECTOR_OFFSET = 4;
    private static final int CANVAS_RIGHT_MARGIN = 24;
    private static final int CANVAS_BOTTOM_MARGIN = 14;

    private static final String CONTAINER_RECT = """
            \t\t<rect fill="#FFFFC0" x="%s" width="%s" height="%s"/>
            """;
    private static final String CONTAINER_BORDER = """
            \t\t<rect fill="none" stroke="gray" stroke-width="1" stroke-linecap="square" stroke-miterlimit="4" transform="translate(0.5 0.5)" x="%s" width="%s" height="%s"/>
            """;
    private static final String EXPAND_BOX = """
            \t\t<rect x="%s" y="%s" width="11" height="11"/>
            \t\t<rect fill="white" x="%s" y="%s" width="9" height="9"/>
            \t\t<rect x="%s" y="%s" width="7" height="1"/>
            """;

    private final SegoeUiMetrics metrics = SegoeUiMetrics.instance();
    private final Connector connector;
    private final TypeNode typeNode;
    private final GroupNode groupNode;
    private final ElementNode elementNode;

    public ExtensionPage(Writer writer, RenderOptions options) {
        super(writer, options);
        this.connector = new Connector(writer);
        this.typeNode = new TypeNode(writer, this.options);
        this.groupNode = new GroupNode(writer, this.options);
        this.elementNode = new ElementNode(writer, this.options);
    }

    public void render(int pageNumber, String derivedName, String derivedDocumentation, String baseLabel,
                       ExtensionSide base, ExtensionSide derivedSide) {
        int derivedWidth = NodeGeometry.nodeWidth(derivedName, 18);
        int derivedDocRight = derivedDocumentation == null ? 0
                : 5 + Documentation.of(derivedDocumentation).width();
        int leftWidth = Math.max(derivedWidth, derivedDocRight);
        int containerX = leftWidth + NODE_GAP;
        int bodyDx = containerX + CONTAINER_PAD;
        int bodyDy = TITLE_HEIGHT;

        CompositeBody body = new CompositeBody(
                writer, options, bodyDx, bodyDy,
                base.style() == null ? null : base.style().compositor(),
                base.attributes(), base.children());
        CompositeBody.Bounds bodyBounds = body.bounds();
        float baseLabelWidth = metrics.advanceWidth(baseLabel, 12f, SegoeUiMetrics.FAMILY, "600", "normal");
        int titleWidth = (int) Math.ceil(baseLabelWidth
                + metrics.advanceWidth(EXTENSION_SUFFIX, 12f, SegoeUiMetrics.FAMILY, "400", "normal"));
        int containerRight = Math.max(bodyBounds.maxX() + CONTAINER_PAD,
                containerX + TITLE_INSET + titleWidth + CONTAINER_PAD);
        int containerBottom = bodyBounds.maxY() + CONTAINER_PAD;

        int branchX = body.branchX();
        int groupX = body.groupX();
        int groupCenter = body.groupCenter();
        int connectorY;
        if (!body.hasGroup()) {
            connectorY = body.hasAttributes() ? body.attributeCenter() : body.containerCenter();
        } else if (body.hasAttributes()) {
            connectorY = (body.attributeCenter() + groupCenter) / 2;
        } else {
            connectorY = groupCenter;
        }
        int spineY = body.hasGroup() ? groupCenter : connectorY;
        int derivedY = connectorY - 10;

        AttributeBox derivedAttributeBox = derivedSide.attributes().isEmpty() ? null
                : new AttributeBox(writer, options, bodyDx, containerBottom + DERIVED_GAP, derivedSide.attributes());
        int derivedChildrenTop = derivedAttributeBox == null
                ? containerBottom + DERIVED_GAP
                : derivedAttributeBox.bottom() + DERIVED_GAP;
        Derived derived = derivedSide.children().isEmpty() ? null
                : layoutDerived(bodyDx, derivedChildrenTop, derivedSide.children());

        int canvasWidth = containerRight + CANVAS_RIGHT_MARGIN;
        int canvasHeight = containerBottom + CANVAS_BOTTOM_MARGIN;
        if (derivedAttributeBox != null) {
            canvasWidth = Math.max(canvasWidth, derivedAttributeBox.right() + CANVAS_RIGHT_MARGIN);
            canvasHeight = Math.max(canvasHeight, derivedAttributeBox.bottom() + CANVAS_BOTTOM_MARGIN);
        }
        if (derived != null) {
            canvasWidth = Math.max(canvasWidth, derived.right() + CANVAS_RIGHT_MARGIN);
            canvasHeight = Math.max(canvasHeight, derived.bottom() + CANVAS_BOTTOM_MARGIN);
        }
        open(pageNumber, canvasWidth, canvasHeight);

        write(CONTAINER_RECT.formatted(containerX, containerRight - containerX, containerBottom));
        write(CONTAINER_BORDER.formatted(containerX, containerRight - containerX - 1, containerBottom - 1));
        Text.write(writer, options, containerX + TITLE_INSET, TITLE_BASELINE, baseLabel, 12f, "600", null, "gray");
        Text.write(writer, options, containerX + TITLE_INSET + baseLabelWidth, TITLE_BASELINE,
                EXTENSION_SUFFIX, 12f, null, null, "gray");

        connector.line(derivedWidth + CONNECTOR_OFFSET, connectorY, branchX - 1, connectorY);
        if (body.hasGroup() && body.hasAttributes()) {
            connector.line(branchX, connectorY, branchX, body.attributeCenter() + 1);
        }
        if (body.hasAttributes()) {
            connector.line(branchX, body.attributeCenter(), groupX - 1, body.attributeCenter());
        }
        if (body.hasGroup()) {
            connector.line(branchX, connectorY, branchX, groupCenter - 1);
            connector.line(branchX, groupCenter, groupX - 1, groupCenter);
        }
        if (derivedAttributeBox != null) {
            int boxCenter = derivedAttributeBox.center();
            connector.line(branchX, spineY, branchX, boxCenter);
            connector.line(branchX, boxCenter, bodyDx - 1, boxCenter);
        }
        if (derived != null) {
            connector.line(branchX, spineY, branchX, derived.groupCenter());
            connector.line(branchX, derived.groupCenter(), derived.groupX() - 1, derived.groupCenter());
        }

        body.draw();
        if (derivedAttributeBox != null) {
            derivedAttributeBox.draw();
        }

        if (derived != null) {
            writeDerivedConnectors(derived);
            groupNode.render(derived.groupX(), derived.groupY(), derivedSide.style().compositor());
            for (int i = 0; i < derived.children().size(); i++) {
                ElementView child = derived.children().get(i);
                elementNode.render(derived.childX(), derived.tops()[i], child.name(), child.documentation(),
                        child.canExpand(), child.expanded(), child.showsText(), child.occurrence());
            }
            writeExpandBox(derived.groupX() + GROUP_WIDTH - 1, derived.groupY() + 5);
        }

        typeNode.render(0, derivedY, derivedName, derivedDocumentation);
        writeExpandBox(derivedWidth - 6, derivedY + 5);

        close();
    }

    private record Derived(int groupX, int childX, int groupY, int groupCenter, int right, int bottom,
                           int[] tops, int[] consumed,
                           List<ElementView> children) {
    }

    private Derived layoutDerived(int groupX, int top, List<ElementView> children) {
        int childX = groupX + CHILD_FROM_GROUP;
        int[] tops = new int[children.size()];
        int[] consumed = new int[children.size()];
        int y = top;
        int right = groupX + GROUP_WIDTH;
        for (int i = 0; i < children.size(); i++) {
            ElementView child = children.get(i);
            consumed[i] = child.consumed();
            tops[i] = y;
            right = Math.max(right, childX + child.visualRight());
            if (child.documentation() != null) {
                right = Math.max(right, childX + child.documentationRight());
            }
            y += consumed[i] + Metrics.SIBLING_ELEMENT_GAP;
        }
        int groupY = (tops[0] + tops[children.size() - 1]) / 2;
        int bottom = containerBottomOf(children, tops, consumed);
        return new Derived(groupX, childX, groupY, groupY + ROW_HEIGHT / 2, right, bottom, tops, consumed, children);
    }

    private static int containerBottomOf(List<ElementView> children, int[] tops, int[] consumed) {
        int bottom = 0;
        for (int i = 0; i < children.size(); i++) {
            boolean repeated = children.get(i).occurrence().isRepeated();
            bottom = Math.max(bottom, tops[i] + consumed[i] + (repeated ? 10 : 14));
        }
        return bottom;
    }

    private void writeDerivedConnectors(Derived derived) {
        List<ElementView> children = derived.children();
        int groupX = derived.groupX();
        int groupCenter = derived.groupCenter();
        int childX = derived.childX();
        int[] tops = derived.tops();
        boolean single = children.size() == 1;
        if (single) {
            connector.line(groupX + GROUP_WIDTH - 1 + EXPAND_BOX_WIDTH, groupCenter, childX - 1, groupCenter);
            return;
        }
        int branchX = groupX + BRANCH_FROM_GROUP;
        List<ChildConnectors.Child> childCenters = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            childCenters.add(new ChildConnectors.Child(children.get(i).occurrence().min(), tops[i] + ROW_HEIGHT / 2));
        }
        ChildConnectors.drawBranch(connector, groupX + GROUP_WIDTH - 1 + EXPAND_BOX_WIDTH,
                groupCenter, branchX, childX, childCenters);
    }

    private void writeExpandBox(int boxX, int boxY) {
        write(EXPAND_BOX.formatted(boxX, boxY, boxX + 1, boxY + 1, boxX + 2, boxY + 5));
    }

}
