package org.jxsd.rendering;

import java.io.Writer;
import org.jxsd.model.Compositor;
import org.jxsd.model.Occurrence;

/**
 * Renders a reference-style group node. Sequence groups are an
 * octagon with a horizontal line and three dots; the geometry is recovered
 * from the reference pages.
 */
public final class GroupNode extends SvgPage {

    private static final String OUTLINE = "fill=\"none\" stroke=\"black\" stroke-width=\"1\" "
            + "stroke-linecap=\"square\" stroke-miterlimit=\"4\" transform=\"translate(0.5 0.5)\"";

    private static final int WIDTH = 35;
    private static final int HEIGHT = 21;
    private static final int SHADOW_OFFSET = 4;

    private static final String FILL_PATH = """
            \t\t<path fill="%s" d="%s"/>
            """;
    private static final String OUTLINE_PATH = """
            \t\t<path %s d="%s"/>
            """;
    private static final String SEGMENT = """
            \t\t<path %s d="M%s %sL%s %s"/>
            """;
    private static final String TRIANGLE = """
            \t\t<path %s d="M%s %sL%s %sL%s %s"/>
            """;
    private static final String QUAD = """
            \t\t<path %s d="M%s %sL%s %sL%s %sL%s %s"/>
            """;
    private static final String CHECKMARK = """
            \t\t<path %s d="M%s %sL%s %sL%s %s"/>
            """;
    private static final String SMALL_RECT = """
            \t\t<rect x="%s" y="%s" width="3" height="3"/>
            """;

    public GroupNode(Writer writer) {
        this(writer, RenderOptions.DEFAULT);
    }

    public GroupNode(Writer writer, RenderOptions options) {
        super(writer, options);
    }

    public void render(int x, int y, Compositor groupType) {
        render(x, y, groupType, Occurrence.SINGLE);
    }

    /**
     * Renders the group node. An unbounded occurrence is drawn as a base copy
     * plus an offset copy, a corner check and the {@code min..∞} indicator.
     */
    public void render(int x, int y, Compositor groupType, Occurrence occurrence) {
        render(x, y, groupType, occurrence, null);
    }

    /**
     * Renders a named group reference as a variable-width octagon carrying the
     * referenced group's name, matching the XMLSpy reference (no compositor glyph).
     */
    public void renderNamed(int x, int y, int width, String name, Occurrence occurrence,
                            String rawDocumentation) {
        String shadow = Shapes.path(Shapes.ShapeKind.GROUP,
                x + SHADOW_OFFSET, y + SHADOW_OFFSET, width, HEIGHT);
        String fill = Shapes.path(Shapes.ShapeKind.GROUP, x, y, width, HEIGHT);
        write(FILL_PATH.formatted("silver", shadow));
        write(FILL_PATH.formatted("white", fill));
        if (rawDocumentation != null && !rawDocumentation.isEmpty()) {
            writeDocumentation(x, y, rawDocumentation);
        }
        Text.write(writer, options, x + 5, y + 15, name, 12f, null, null, null);
        write(OUTLINE_PATH.formatted(OUTLINE, Shapes.outline(Shapes.ShapeKind.GROUP, x, y, width, HEIGHT)));
        if (occurrence.isRepeated()) {
            writeOccurrence(x + width - 1, y, occurrence);
        }
        flush();
    }

    /** Renders the group with an optional documentation panel below its box. */
    public void render(int x, int y, Compositor groupType, Occurrence occurrence,
                       String rawDocumentation) {
        if (groupType != Compositor.SEQUENCE && groupType != Compositor.CHOICE
                && groupType != Compositor.ALL) {
            throw new IllegalArgumentException("Unsupported group type: " + groupType);
        }
        boolean repeated = occurrence.isRepeated();
        if (repeated) {
            String stroke = Shapes.SOLID_STROKE;
            String shadow = Shapes.path(Shapes.ShapeKind.GROUP,
                    x + SHADOW_OFFSET + 3, y + SHADOW_OFFSET + 3, WIDTH, HEIGHT);
            String copyFill = Shapes.outline(Shapes.ShapeKind.GROUP, x + 3, y + 3, WIDTH, HEIGHT);
            String copyOutline = copyFill;
            write(FILL_PATH.formatted("silver", shadow));
            write(FILL_PATH.formatted("white", copyFill));
            write(OUTLINE_PATH.formatted(stroke, copyOutline));
            write(FILL_PATH.formatted("white", Shapes.path(Shapes.ShapeKind.GROUP, x, y, WIDTH, HEIGHT)));
            writeCheckmark(x + WIDTH - 16, y + HEIGHT);
            writeGlyph(x, y, groupType);
            writeOccurrence(x + WIDTH - 1, y, occurrence);
            write(OUTLINE_PATH.formatted(stroke, Shapes.outline(Shapes.ShapeKind.GROUP, x, y, WIDTH, HEIGHT)));
            flush();
            return;
        }
        String fill = Shapes.path(Shapes.ShapeKind.GROUP, x, y, WIDTH, HEIGHT);
        String shadow = Shapes.path(Shapes.ShapeKind.GROUP,
                x + SHADOW_OFFSET, y + SHADOW_OFFSET, WIDTH, HEIGHT);
        String outline = Shapes.outline(Shapes.ShapeKind.GROUP, x, y, WIDTH, HEIGHT);

        write(FILL_PATH.formatted("silver", shadow));
        write(FILL_PATH.formatted("white", fill));
        if (rawDocumentation != null && !rawDocumentation.isEmpty()) {
            writeDocumentation(x, y, rawDocumentation);
        }
        writeGlyph(x, y, groupType);
        write(OUTLINE_PATH.formatted(Shapes.SOLID_STROKE, outline));
        flush();
    }

    private void writeDocumentation(int x, int y, String rawDocumentation) {
        Documentation documentation = Documentation.of(rawDocumentation);
        for (int i = 0; i < documentation.lines().size(); i++) {
            Documentation.Line line = documentation.lines().get(i);
            Text.write(writer, options, x + 5,
                    y + 37 + i * Documentation.LINE_HEIGHT,
                    line.text(), 9f, null, null, "gray");
        }
    }

    private void writeGlyph(int x, int y, Compositor groupType) {
        if (groupType == Compositor.SEQUENCE) {
            int middle = y + HEIGHT / 2;
            write(SEGMENT.formatted(OUTLINE, x + 5, middle, x + WIDTH - 6, middle));
            for (int offset : new int[] {11, 16, 21}) {
                write(SMALL_RECT.formatted(x + offset, middle - 1));
            }
        } else if (groupType == Compositor.ALL) {
            write(SEGMENT.formatted(OUTLINE, x + 5, y + 10, x + 14, y + 10));
            write(QUAD.formatted(OUTLINE, x + 14, y + 6, x + 10, y + 6, x + 10, y + 14, x + 14, y + 14));
            write(SEGMENT.formatted(OUTLINE, x + 20, y + 10, x + 28, y + 10));
            write(QUAD.formatted(OUTLINE, x + 20, y + 6, x + 24, y + 6, x + 24, y + 14, x + 20, y + 14));
            for (int offset : new int[] {5, 9, 13}) {
                write(SMALL_RECT.formatted(x + 16, y + offset));
            }
        } else {
            write(TRIANGLE.formatted(OUTLINE, x + 5, y + 10, x + 10, y + 10, x + 14, y + 6));
            write(SEGMENT.formatted(OUTLINE, x + 20, y + 10, x + 28, y + 10));
            write(QUAD.formatted(OUTLINE, x + 20, y + 6, x + 24, y + 6, x + 24, y + 14, x + 20, y + 14));
            for (int offset : new int[] {5, 9, 13}) {
                write(SMALL_RECT.formatted(x + 16, y + offset));
            }
        }
    }

    private void writeCheckmark(int x, int y) {
        write(CHECKMARK.formatted(OUTLINE, x, y, x + 6, y + 6, x + 9, y + 3));
    }

    private void writeOccurrence(int baseRight, int y, Occurrence occurrence) {
        OccurrenceIndicator.write(writer, options, baseRight, y, occurrence);
    }
}
