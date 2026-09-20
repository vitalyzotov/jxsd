package org.jxsd.rendering;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.jxsd.model.Compositor;
import org.jxsd.model.Occurrence;

/**
 * Renders a reference-style page for a content chain
 * {@code complexType -> group -> element+}. Assembles the node primitives and
 * reproduces the reference pages for this shape family, including the
 * vertical branch connectors used for multiple children.
 */
public final class ChainPage extends SvgPage {

    private static final int GROUP_WIDTH = 35;
    private static final int NODE_GAP = 10;
    private static final int CONNECTOR_OFFSET = 4;
    private static final int CANVAS_RIGHT_MARGIN = 24;
    private static final int DOC_RIGHT_MARGIN = 20;
    private static final int ROW_HEIGHT = 21;
    private static final int BRANCH_FROM_GROUP = 54;
    private static final int CHILD_FROM_BRANCH = 15;
    private static final int EXPAND_BOX_WIDTH = 11;
    private static final int GROUP_SERIES_GAP = 47;
    private static final int GROUP_SERIES_CONNECTOR_OFFSET = 10;

    private static final String EXPAND_BOX = """
            \t\t<rect x="%s" y="%s" width="11" height="11"/>
            \t\t<rect fill="white" x="%s" y="%s" width="9" height="9"/>
            \t\t<rect x="%s" y="%s" width="7" height="1"/>
            """;

    private final Connector connector;
    private final TypeNode typeNode;
    private final GroupNode groupNode;
    private final ElementNode elementNode;

    public ChainPage(Writer writer) {
        this(writer, RenderOptions.DEFAULT);
    }

    public ChainPage(Writer writer, RenderOptions options) {
        super(writer, options);
        this.connector = new Connector(writer);
        this.typeNode = new TypeNode(writer, options);
        this.groupNode = new GroupNode(writer, options);
        this.elementNode = new ElementNode(writer, options);
    }

    public void render(int pageNumber, String typeName, String typeDocumentation, Compositor groupType,
                       List<ElementView> children) {
        render(pageNumber, typeName, typeDocumentation, groupType, Occurrence.SINGLE, children);
    }

    public void render(int pageNumber, String typeName, String typeDocumentation, Compositor groupType,
                       Occurrence groupOccurrence, List<ElementView> children) {
        render(pageNumber, typeName, typeDocumentation,
                List.of(new GroupSpec(groupType, groupOccurrence)), children);
    }

    /** Renders a type whose content is a series of groups before the element children. */
    public void render(int pageNumber, String typeName, String typeDocumentation, List<GroupSpec> groups,
                       List<ElementView> children) {
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("At least one group is required.");
        }
        int typeWidth = NodeGeometry.nodeWidth(typeName, 18);
        Documentation typeDoc = typeDocumentation == null ? null
                : Documentation.of(typeDocumentation);
        int typeDocumentationRight = typeDoc == null ? 0 : 5 + typeDoc.width();
        int firstGroupX = groupPlacement(typeWidth, typeDocumentation, typeDocumentationRight);
        int lastGroupX = firstGroupX + (groups.size() - 1) * GROUP_SERIES_GAP;
        GroupSpec lastGroup = groups.getLast();

        boolean single = children.size() == 1;
        int childX = lastGroupX + BRANCH_FROM_GROUP + CHILD_FROM_BRANCH;

        int[] tops = new int[children.size()];
        int[] consumed = new int[children.size()];
        int y = 0;
        for (int i = 0; i < children.size(); i++) {
            ElementView child = children.get(i);
            consumed[i] = child.consumed();
            tops[i] = y;
            y += consumed[i] + Metrics.SIBLING_ELEMENT_GAP;
        }
        int groupY = children.isEmpty() ? 0 : (tops[0] + tops[children.size() - 1]) / 2;
        int groupCenter = groupY + ROW_HEIGHT / 2;

        int canvasWidth = typeDoc == null ? 0 : typeDocumentationRight + DOC_RIGHT_MARGIN;
        int canvasHeight = typeDoc == null ? 0 : groupY + 27 + NodeGeometry.emittedHeight(typeDoc) + 10;
        for (int i = 0; i < children.size(); i++) {
            ElementView child = children.get(i);
            canvasWidth = Math.max(canvasWidth, childX + child.visualRight() + CANVAS_RIGHT_MARGIN);
            if (child.documentation() != null) {
                canvasWidth = Math.max(canvasWidth,
                        childX + child.documentationRight() + DOC_RIGHT_MARGIN);
            }
            canvasHeight = Math.max(canvasHeight, tops[i] + consumed[i] + child.bottomMargin());
        }
        canvasWidth = Math.max(canvasWidth, lastGroupX + GROUP_WIDTH + CANVAS_RIGHT_MARGIN);
        canvasHeight = Math.max(canvasHeight, groupY + ROW_HEIGHT + 14);
        for (GroupSpec group : groups) {
            if (group.occurrence().isRepeated()) {
                canvasHeight = Math.max(canvasHeight, groupY + 43 + 10);
            }
        }
        open(pageNumber, canvasWidth, canvasHeight);

        connector.line(typeWidth + CONNECTOR_OFFSET, groupCenter, firstGroupX - 1, groupCenter);
        typeNode.render(0, groupY, typeName, typeDocumentation);
        for (int i = 1; i < groups.size(); i++) {
            int previousX = firstGroupX + (i - 1) * GROUP_SERIES_GAP;
            int currentX = firstGroupX + i * GROUP_SERIES_GAP;
            connector.line(previousX + GROUP_WIDTH + GROUP_SERIES_CONNECTOR_OFFSET, groupCenter,
                    currentX - 1, groupCenter);
            GroupSpec previous = groups.get(i - 1);
            groupNode.render(previousX, groupY, previous.style().compositor(), previous.occurrence());
        }
        writeConnectors(lastGroupX, groupCenter, childX, children, tops, single);
        groupNode.render(lastGroupX, groupY, lastGroup.style().compositor(), lastGroup.occurrence());
        for (int i = 0; i < children.size(); i++) {
            ElementView child = children.get(i);
            elementNode.render(childX, tops[i], child.name(), child.documentation(),
                    child.canExpand(), child.expanded(), child.showsText(),
                    child.occurrence());
        }

        for (int i = groups.size() - 1; i >= 0; i--) {
            writeExpandBox(firstGroupX + i * GROUP_SERIES_GAP + GROUP_WIDTH - 1, groupY + 5);
        }
        writeExpandBox(typeWidth - 6, groupY + 5);

        close();
    }

    private static int expandBoxRight(int groupX) {
        return groupX + GROUP_WIDTH - 1 + EXPAND_BOX_WIDTH;
    }

    private void writeConnectors(int groupX, int groupCenter, int childX, List<ElementView> children,
                                 int[] tops, boolean single) {
        if (single) {
            connector.line(expandBoxRight(groupX), groupCenter, childX - 1, groupCenter);
            return;
        }
        int branchX = groupX + BRANCH_FROM_GROUP;
        List<ChildConnectors.Child> childCenters = new ArrayList<>();
        for (int i = 0; i < children.size(); i++) {
            childCenters.add(new ChildConnectors.Child(children.get(i).occurrence().min(), tops[i] + ROW_HEIGHT / 2));
        }
        ChildConnectors.drawBranch(connector, expandBoxRight(groupX), groupCenter, branchX, childX, childCenters);
    }

    private static int groupPlacement(int typeWidth, String typeDocumentation, int typeDocumentationRight) {
        if (typeDocumentation == null) {
            return typeWidth + 6;
        }
        if (typeDocumentationRight <= typeWidth) {
            return typeWidth + NODE_GAP;
        }
        return typeDocumentationRight + 7;
    }

    private void writeExpandBox(int boxX, int boxY) {
        write(EXPAND_BOX.formatted(boxX, boxY, boxX + 1, boxY + 1, boxX + 2, boxY + 5));
    }
}
