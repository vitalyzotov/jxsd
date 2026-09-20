package org.jxsd.rendering;

import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import org.jxsd.model.Occurrence;

/**
 * Renders a reference-style page for a complex type shown inline
 * inside a yellow context box: the referencing element on the left, then the
 * type name as a title and the group with its children, all laid out like a
 * chain page but wrapped in the recovered {@code #FFFFC0} container.
 */
public final class ContextPage extends SvgPage {

    private static final String OUTLINE = "fill=\"none\" stroke=\"black\" stroke-width=\"1\" "
            + "stroke-linecap=\"square\" stroke-miterlimit=\"4\" transform=\"translate(0.5 0.5)\"";

    private static final int ROW_HEIGHT = 21;
    private static final int GROUP_WIDTH = 35;
    private static final int CONTENT_TOP = 26;
    private static final int NODE_GAP = 7;
    private static final int GROUP_INSET = 3;
    private static final int BRANCH_GAP = 69;
    private static final int EXPAND_BOX_WIDTH = 11;
    private static final int GROUP_SERIES_GAP = 47;
    private static final int GROUP_SERIES_CONNECTOR_OFFSET = 10;
    private static final int CONNECTOR_START_GAP = 5;
    private static final int CONNECTOR_END_GAP = 2;
    private static final int SHADOW_OFFSET = 4;
    private static final int CONTAINER_RIGHT_PAD = 6;
    private static final int CONTAINER_BOTTOM_PAD = 2;
    private static final int CANVAS_RIGHT_MARGIN = 20;
    private static final int CANVAS_BOTTOM_MARGIN = 10;
    private static final int BRANCH_FROM_GROUP = 54;
    private static final int CHILD_FROM_BRANCH = 15;
    private static final int TITLE_INSET_X = 5;
    private static final int TITLE_Y = 8;
    private static final int TITLE_HEIGHT = 16;
    private static final int TITLE_BASELINE = 21;
    private static final int DOC_INSET_X = 5;
    private static final int DOC_BG_Y = 27;
    private static final int DOC_REPEATED_BG_Y = 46;
    private static final int DOC_BG_HEIGHT = 12;

    private static final String CONTAINER_RECT = """
            \t\t<rect fill="#FFFFC0" x="%s" width="%s" height="%s"/>
            """;
    private static final String CONTAINER_BORDER = """
            \t\t<rect fill="none" stroke="gray" stroke-width="1" stroke-linecap="square" stroke-miterlimit="4" transform="translate(0.5 0.5)" x="%s" width="%s" height="%s"/>
            """;
    private static final String TITLE_RECT = """
            \t\t<rect fill="#FFFFC0" x="%s" y="%s" width="%s" height="%s"/>
            """;
    private static final String SILVER_RECT = """
            \t\t<rect fill="silver" x="%s" y="%s" width="%s" height="%s"/>
            """;
    private static final String WHITE_FIXED_RECT = """
            \t\t<rect fill="white" x="%s" y="%s" width="%s" height="%s"/>
            """;
    private static final String WHITE_Y_RECT = """
            \t\t<rect fill="white" y="%s" width="%s" height="%s"/>
            """;
    private static final String OUTLINE_FIXED_RECT = """
            \t\t<rect %s x="%s" y="%s" width="%s" height="20"/>
            """;
    private static final String OUTLINE_Y_RECT = """
            \t\t<rect %s y="%s" width="%s" height="20"/>
            """;
    private static final String NAME_BG_RECT = """
            \t\t<rect fill="white" x="9" y="%s" width="%s" height="16"/>
            """;
    private static final String CHECKMARK = """
            \t\t<path %s d="M%s %sL%s %sL%s %s"/>
            """;
    private static final String SEGMENT = """
            \t\t<path %s d="M%s %sL%s %s"/>
            """;
    private static final String EXPAND_BOX = """
            \t\t<rect x="%s" y="%s" width="11" height="11"/>
            \t\t<rect fill="white" x="%s" y="%s" width="9" height="9"/>
            \t\t<rect x="%s" y="%s" width="7" height="1"/>
            """;
    private static final String EXPAND_PLUS = """
            \t\t<rect x="%s" y="%s" width="1" height="7"/>
            """;

    private final SegoeUiMetrics metrics = SegoeUiMetrics.instance();
    private final Connector connector;
    private final GroupNode groupNode;
    private final ElementNode childNode;

    public ContextPage(Writer writer) {
        this(writer, RenderOptions.DEFAULT);
    }

    public ContextPage(Writer writer, RenderOptions options) {
        super(writer, options);
        this.connector = new Connector(writer);
        this.groupNode = new GroupNode(writer, options);
        this.childNode = new ElementNode(writer, options);
    }

    /**
     * Renders the referencing element, the yellow type container (title and
     * optional attributes) and the group children.
     */
    public void render(int pageNumber, ElementView element, String typeName,
                       List<GroupSpec> groups, List<ElementView> children,
                       List<AttributeNode> attributes) {
        boolean elementRepeated = element.occurrence().isRepeated();
        int elementGlyphSpace = element.canExpand() ? 21 : 16;
        int elementWidth = NodeGeometry.nodeWidth(element.name(), elementGlyphSpace);
        int elementBackground = elementWidth - elementGlyphSpace;
        Documentation elementDoc = element.documentation() == null ? null
                : Documentation.of(element.documentation());
        int elementDocRight = elementDoc == null ? 0 : DOC_INSET_X + elementDoc.width();

        int placement = elementWidth + NODE_GAP;
        if (elementDoc != null && !elementRepeated) {
            placement += 4;
        }
        int yellowX = Math.max(placement, elementDocRight + NODE_GAP);
        int firstGroupX = yellowX + GROUP_INSET;
        int groupX = groups.isEmpty() ? firstGroupX : firstGroupX + (groups.size() - 1) * GROUP_SERIES_GAP;
        int titleWidth = titleWidth(typeName);

        boolean single = children.size() == 1;
        int childX = groupX + BRANCH_GAP;

        AttributeBox attributeBox = attributes.isEmpty() ? null
                : new AttributeBox(writer, options, firstGroupX, CONTENT_TOP, attributes);
        int contentTop = attributeBox == null ? CONTENT_TOP : attributeBox.bottom() + GROUP_INSET;

        int[] tops = new int[children.size()];
        int[] consumed = new int[children.size()];
        int y = contentTop;
        for (int i = 0; i < children.size(); i++) {
            ElementView child = children.get(i);
            consumed[i] = child.consumed();
            tops[i] = y;
            y += consumed[i] + Metrics.SIBLING_ELEMENT_GAP;
        }
        int groupY;
        if (!children.isEmpty()) {
            groupY = (tops[0] + tops[children.size() - 1]) / 2;
        } else if (attributeBox != null) {
            groupY = attributeBox.center() - ROW_HEIGHT / 2;
        } else {
            groupY = contentTop;
        }
        int groupCenter = groupY + ROW_HEIGHT / 2;
        int elementY = groupY;

        int contentRight = groupRight(groupX);
        int titleRight = firstGroupX + TITLE_INSET_X + titleWidth;
        int contentBottom = groupY + ROW_HEIGHT;
        if (attributeBox != null) {
            contentRight = Math.max(contentRight, attributeBox.right());
            contentBottom = Math.max(contentBottom, attributeBox.bottom());
        }
        for (int i = 0; i < children.size(); i++) {
            ElementView child = children.get(i);
            boolean repeated = child.occurrence().isRepeated();
            contentRight = Math.max(contentRight, childX + child.visualRight() + SHADOW_OFFSET);
            if (child.documentation() != null) {
                contentRight = Math.max(contentRight, childX + child.documentationRight());
            }
            int shadowBottom = tops[i] + (repeated ? 3 : 0) + ROW_HEIGHT + SHADOW_OFFSET;
            contentBottom = Math.max(contentBottom, Math.max(tops[i] + consumed[i], shadowBottom));
        }
        int containerRight = Math.max(contentRight + CONTAINER_RIGHT_PAD, titleRight + CONTAINER_RIGHT_PAD + 1);
        int containerBottom = contentBottom + CONTAINER_BOTTOM_PAD;

        int elementVisualRight = (elementRepeated ? 3 : 0) + elementWidth;
        int elementDocBottom = elementDoc == null ? 0
                : elementY + (elementRepeated ? DOC_REPEATED_BG_Y : DOC_BG_Y) + elementDoc.height(DOC_BG_HEIGHT);
        int elementBottom = Math.max(elementY + ROW_HEIGHT, elementDocBottom);
        int canvasWidth = Math.max(Math.max(contentRight + CONTAINER_RIGHT_PAD + CANVAS_RIGHT_MARGIN,
                        titleRight + CONTAINER_RIGHT_PAD + CANVAS_RIGHT_MARGIN + 3),
                Math.max(elementVisualRight, elementDocRight) + CANVAS_RIGHT_MARGIN);
        int canvasHeight = Math.max(containerBottom, elementBottom) + CANVAS_BOTTOM_MARGIN;

        open(pageNumber, canvasWidth, canvasHeight);
        write(CONTAINER_RECT.formatted(yellowX, containerRight - yellowX, containerBottom));
        write(CONTAINER_BORDER.formatted(yellowX, containerRight - yellowX - 1, containerBottom - 1));

        connector.line(elementWidth + CONNECTOR_START_GAP, groupCenter, yellowX + CONNECTOR_END_GAP, groupCenter);
        writeElement(element, elementY, elementWidth, elementBackground,
                elementDoc, elementDoc == null ? 0 : elementY
                        + (elementRepeated ? DOC_REPEATED_BG_Y : DOC_BG_Y),
                typeName, firstGroupX + TITLE_INSET_X, titleWidth);
        if (attributeBox != null) {
            attributeBox.draw();
        }

        for (int i = 1; i < groups.size(); i++) {
            int previousX = firstGroupX + (i - 1) * GROUP_SERIES_GAP;
            int currentX = firstGroupX + i * GROUP_SERIES_GAP;
            connector.line(previousX + GROUP_WIDTH + GROUP_SERIES_CONNECTOR_OFFSET, groupCenter,
                    currentX - 1, groupCenter);
            GroupSpec previous = groups.get(i - 1);
            groupNode.render(previousX, groupY, previous.style().compositor(), previous.occurrence());
        }
        if (!children.isEmpty()) {
            writeConnectors(groupX, groupCenter, childX, children, tops, single);
        }
        if (!groups.isEmpty()) {
            GroupSpec last = groups.getLast();
            groupNode.render(groupX, groupY, last.style().compositor(), last.occurrence());
        }
        for (int i = 0; i < children.size(); i++) {
            ElementView child = children.get(i);
            childNode.render(childX, tops[i], child.name(), child.documentation(),
                    child.canExpand(), child.expanded(), child.showsText(),
                    child.occurrence());
        }

        for (int i = groups.size() - 1; i >= 0; i--) {
            writeExpandBox(firstGroupX + i * GROUP_SERIES_GAP + GROUP_WIDTH - 1, groupY + 5, true);
        }
        writeExpandBox(elementWidth - 6, groupY + 5, true);

        close();
    }

    private void writeElement(ElementView element, int y, int width, int backgroundWidth,
                              Documentation documentation, int documentationY,
                              String titleName, int titleX, int titleWidth) {
        boolean repeated = element.occurrence().isRepeated();
        if (repeated) {
            int mainX = 3;
            int mainY = y + 3;
            write(SILVER_RECT.formatted(mainX + 4, mainY + 4, width, ROW_HEIGHT));
            write(WHITE_FIXED_RECT.formatted(mainX, mainY, width, ROW_HEIGHT));
            write(OUTLINE_FIXED_RECT.formatted(Shapes.SOLID_STROKE, mainX, mainY, width - 1));
            write(WHITE_Y_RECT.formatted(y, width, ROW_HEIGHT));
            writeCheckmark(width - 15, y + ROW_HEIGHT);
        } else {
            write(SILVER_RECT.formatted(4, y + 4, width, ROW_HEIGHT));
            write(WHITE_Y_RECT.formatted(y, width, ROW_HEIGHT));
        }
        writeTitle(titleX, titleWidth, titleName);
        if (documentation != null) {
            writeDocumentation(documentation, documentationY);
        }
        write(NAME_BG_RECT.formatted(y + 2, backgroundWidth));
        writeName(element.name(), y);
        if (element.showsText()) {
            writeSimpleContentGlyph(0, y);
        }
        if (repeated) {
            writeOccurrence(width, y, element.occurrence());
        }
        write(OUTLINE_Y_RECT.formatted(Shapes.SOLID_STROKE, y, width - 1));
    }

    private void writeDocumentation(Documentation documentation, int bgY) {
        for (int i = 0; i < documentation.lines().size(); i++) {
            Documentation.Line line = documentation.lines().get(i);
            Text.write(writer, options, DOC_INSET_X,
                    bgY + 10 + i * Documentation.LINE_HEIGHT, line.text(), 9f, null, null, "gray");
        }
    }

    private void writeTitle(int x, int width, String typeName) {
        if (typeName.isEmpty()) {
            return;
        }
        write(TITLE_RECT.formatted(x, TITLE_Y, width, TITLE_HEIGHT));
        Text.write(writer, options, x, TITLE_BASELINE, typeName, 12f, "600", null, "gray");
    }

    private void writeName(String name, int y) {
        Text.write(writer, options, 9, y + 15, name, 12f, "600", null, null);
    }

    private void writeCheckmark(int x, int y) {
        write(CHECKMARK.formatted(OUTLINE, x, y, x + 6, y + 6, x + 9, y + 3));
    }

    private void writeOccurrence(int baseRight, int y, Occurrence occurrence) {
        OccurrenceIndicator.write(writer, options, baseRight, y, occurrence);
    }

    private void writeSimpleContentGlyph(int x, int y) {
        write(SEGMENT.formatted(OUTLINE, x + 2, y + 2, x + 7, y + 2));
        write(SEGMENT.formatted(OUTLINE, x + 2, y + 4, x + 6, y + 4));
        write(SEGMENT.formatted(OUTLINE, x + 2, y + 6, x + 6, y + 6));
    }

    private void writeConnectors(int groupX, int groupCenter, int childX,
                                 List<ElementView> children, int[] tops, boolean single) {
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

    private void writeExpandBox(int boxX, int boxY, boolean expanded) {
        write(EXPAND_BOX.formatted(boxX, boxY, boxX + 1, boxY + 1, boxX + 2, boxY + 5));
        if (!expanded) {
            write(EXPAND_PLUS.formatted(boxX + 5, boxY + 2));
        }
    }

    private static int groupRight(int groupX) {
        return groupX + GROUP_WIDTH;
    }

    private int titleWidth(String name) {
        return (int) Math.ceil(metrics.advanceWidth(name, 12f, SegoeUiMetrics.FAMILY, "600", "normal")) + 1;
    }

}
