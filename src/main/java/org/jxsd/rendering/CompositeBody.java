package org.jxsd.rendering;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.jxsd.model.Compositor;

/**
 * The body shared by composite pages and {@code xs:extension} containers: an
 * attribute tab above a content group with its element children. Layout is
 * computed relative to an origin, so the body can be drawn at an offset inside a
 * wrapping container.
 */
final class CompositeBody extends SvgPage {

    private static final int GROUP_WIDTH = 35;
    private static final int ROW_HEIGHT = 21;
    private static final int BRANCH_FROM_GROUP = 7;
    private static final int CHILD_BRANCH_FROM_GROUP = 54;
    private static final int CHILD_FROM_GROUP = 69;
    private static final int EXPAND_BOX_WIDTH = 11;
    static final int CANVAS_RIGHT_MARGIN = 24;
    static final int DOC_RIGHT_MARGIN = 20;

    private static final String EXPAND_BOX = """
            \t\t<rect x="%s" y="%s" width="11" height="11"/>
            \t\t<rect fill="white" x="%s" y="%s" width="9" height="9"/>
            \t\t<rect x="%s" y="%s" width="7" height="1"/>
            """;

    /** Absolute bounds of the drawn body. */
    record Bounds(int minX, int minY, int maxX, int maxY) {
    }

    private final Connector connector;
    private final GroupNode groupNode;
    private final ElementNode elementNode;

    private final Compositor groupType;
    private final List<ElementView> children;
    private final boolean hasAttributes;
    private final int dy;

    private final AttributeBox attributeBox;
    private final int groupX;
    private final int childX;
    private final int containerBottom;
    private final int bodyRight;
    private final int[] tops;
    private final int[] consumed;
    private final int groupY;

    CompositeBody(Writer writer, RenderOptions options, int dx, int dy,
                           Compositor groupType,
                           List<AttributeNode> attributes,
                           List<ElementView> children) {
        super(writer, options);
        this.connector = new Connector(writer);
        this.groupNode = new GroupNode(writer, options);
        this.elementNode = new ElementNode(writer, options);
        this.groupType = groupType;
        this.children = children;
        this.hasAttributes = !attributes.isEmpty();
        this.dy = dy;

        this.groupX = dx;
        this.childX = dx + CHILD_FROM_GROUP;

        this.attributeBox = hasAttributes
                ? new AttributeBox(writer, options, dx, dy, attributes) : null;
        this.containerBottom = hasAttributes ? attributeBox.bottom() : dy;
        this.bodyRight = hasAttributes ? attributeBox.right() : groupX + GROUP_WIDTH;

        this.tops = new int[children.size()];
        this.consumed = new int[children.size()];
        int y = hasAttributes ? containerBottom + 3 : dy;
        for (int i = 0; i < children.size(); i++) {
            ElementView child = children.get(i);
            consumed[i] = child.consumed();
            tops[i] = y;
            y += consumed[i] + Metrics.SIBLING_ELEMENT_GAP;
        }
        this.groupY = children.isEmpty() ? dy : (tops[0] + tops[children.size() - 1]) / 2;
    }

    int groupX() {
        return groupX;
    }

    int branchX() {
        return groupX - BRANCH_FROM_GROUP;
    }

    int groupCenter() {
        return groupY + ROW_HEIGHT / 2;
    }

    int containerCenter() {
        return (containerBottom + 1) / 2;
    }

    boolean hasAttributes() {
        return hasAttributes;
    }

    /** Vertical centre of the attribute tab, honouring the body's y offset. */
    int attributeCenter() {
        return attributeBox.center();
    }

    int canvasWidth(int typeDocumentationRight) {
        int width = typeDocumentationRight + DOC_RIGHT_MARGIN;
        width = Math.max(width, bodyRight + DOC_RIGHT_MARGIN);
        for (int i = 0; i < children.size(); i++) {
            ElementView child = children.get(i);
            width = Math.max(width, childX + child.visualRight() + CANVAS_RIGHT_MARGIN);
            if (child.documentation() != null) {
                width = Math.max(width, childX + child.documentationRight() + DOC_RIGHT_MARGIN);
            }
        }
        return width;
    }

    int canvasHeight() {
        int height = Math.max(containerBottom + 10, groupY + ROW_HEIGHT + 14);
        for (int i = 0; i < children.size(); i++) {
            height = Math.max(height, tops[i] + consumed[i] + children.get(i).bottomMargin());
        }
        return height;
    }

    Bounds bounds() {
        int minX = branchX() - 1;
        int maxX = bodyRight;
        int maxY = containerBottom;
        for (int i = 0; i < children.size(); i++) {
            ElementView child = children.get(i);
            maxX = Math.max(maxX, childX + child.visualRight());
            if (child.documentation() != null) {
                maxX = Math.max(maxX, childX + child.documentationRight());
            }
            maxY = Math.max(maxY, tops[i] + consumed[i] + child.bottomMargin());
        }
        return new Bounds(minX, dy, maxX, maxY);
    }

    void draw() {
        if (hasAttributes) {
            attributeBox.draw();
        }
        writeChildConnectors();
        groupNode.render(groupX, groupY, groupType);
        for (int i = 0; i < children.size(); i++) {
            ElementView child = children.get(i);
            elementNode.render(childX, tops[i], child.name(), child.documentation(),
                    child.canExpand(), child.expanded(), child.showsText(),
                    child.occurrence());
        }
        writeExpandBox(groupX + GROUP_WIDTH - 1, groupY + 5);
    }

    private void writeChildConnectors() {
        int branchX = groupX + CHILD_BRANCH_FROM_GROUP;
        List<ChildConnectors.Child> childCenters = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            childCenters.add(new ChildConnectors.Child(children.get(i).occurrence().min(), tops[i] + ROW_HEIGHT / 2));
        }
        ChildConnectors.drawBranch(connector, groupX + GROUP_WIDTH - 1 + EXPAND_BOX_WIDTH,
                groupCenter(), branchX, childX, childCenters);
    }

    private void writeExpandBox(int boxX, int boxY) {
        write(EXPAND_BOX.formatted(boxX, boxY, boxX + 1, boxY + 1, boxX + 2, boxY + 5));
    }

}
