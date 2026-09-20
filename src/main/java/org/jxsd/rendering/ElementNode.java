package org.jxsd.rendering;

import java.io.Writer;
import org.jxsd.model.Occurrence;

/**
 * Renders a reference-style element node: a shadowed rectangle with
 * the bold name and an optional gray documentation, plus an expand/collapse
 * box when the element has children. Geometry is recovered from the reference pages.
 */
public final class ElementNode extends SvgPage {

    private static final String OUTLINE = "fill=\"none\" stroke=\"black\" stroke-width=\"1\" "
            + "stroke-linecap=\"square\" stroke-miterlimit=\"4\" transform=\"translate(0.5 0.5)\"";

    private static final String FAMILY = SegoeUiMetrics.FAMILY;
    private static final String NAME_WEIGHT = "600";
    private static final float NAME_SIZE = 12f;
    private static final float DOC_SIZE = 9f;

    private static final int HEIGHT = 21;
    private static final int SHADOW_OFFSET = 4;
    private static final int NAME_INSET_X = 9;
    private static final int NAME_BG_Y = 2;
    private static final int NAME_BASELINE = 15;
    private static final int DOC_INSET_X = 5;
    private static final int DOC_BG_Y = 27;
    private static final int DOC_BASELINE = 37;
    private static final int LEAF_GLYPH_SPACE = 16;
    private static final int EXPAND_GLYPH_SPACE = 21;

    private static final String SHADOW_RECT = """
            \t\t<rect fill="silver" x="%s" y="%s" width="%s" height="%s"/>
            """;
    private static final String WHITE_RECT = """
            \t\t<rect fill="white"%s width="%s" height="%s"/>
            """;
    private static final String NAME_BG_RECT = """
            \t\t<rect fill="white" x="%s" y="%s" width="%s" height="16"/>
            """;
    private static final String OUTLINE_RECT = """
            \t\t<rect %s%s width="%s" height="20"/>
            """;
    private static final String SEGMENT = """
            \t\t<path %s d="M%s %sL%s %s"/>
            """;
    private static final String CHECKMARK = """
            \t\t<path %s d="M%s %sL%s %sL%s %s"/>
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

    public ElementNode(Writer writer) {
        this(writer, RenderOptions.DEFAULT);
    }

    public ElementNode(Writer writer, RenderOptions options) {
        super(writer, options);
    }

    public void render(int x, int y, String name, String rawDocumentation, boolean canExpand, boolean expanded) {
        render(x, y, name, rawDocumentation, canExpand, expanded, false);
    }

    public void render(int x, int y, String name, String rawDocumentation,
                       boolean canExpand, boolean expanded, boolean showsText) {
        render(x, y, name, rawDocumentation, canExpand, expanded, showsText, Occurrence.SINGLE);
    }

    /**
     * Renders the element node. An unbounded occurrence is drawn as a base copy
     * plus an offset copy, a corner check and the {@code min..∞} indicator.
     */
    public void render(int x, int y, String name, String rawDocumentation, boolean canExpand, boolean expanded,
                       boolean showsText, Occurrence occurrence) {
        int glyphSpace = canExpand ? EXPAND_GLYPH_SPACE : LEAF_GLYPH_SPACE;
        int width = NodeGeometry.elementWidth(name, canExpand);
        int backgroundWidth = width - glyphSpace;
        boolean repeated = occurrence.isRepeated();
        if (repeated) {
            renderRepeated(x, y, name, rawDocumentation, canExpand, expanded, showsText, occurrence,
                    width, backgroundWidth);
            flush();
            return;
        }

        write(SHADOW_RECT.formatted(x + SHADOW_OFFSET, y + SHADOW_OFFSET, width, HEIGHT));
        write(WHITE_RECT.formatted(position(x, y), width, HEIGHT));
        if (rawDocumentation != null && !rawDocumentation.isEmpty()) {
            writeDocumentation(x, y, rawDocumentation);
        }
        write(NAME_BG_RECT.formatted(x + NAME_INSET_X, y + NAME_BG_Y, backgroundWidth));
        writeName(x, y, name);
        if (showsText) {
            writeSimpleContentGlyph(x, y);
        }
        write(OUTLINE_RECT.formatted(Shapes.SOLID_STROKE, position(x, y), width - 1));
        if (occurrence.isProhibited()) {
            writeCross(x, y, width, HEIGHT);
        }
        if (canExpand) {
            writeExpandBox(x + width - 6, y + 5, expanded);
        }
        flush();
    }

    /** The cross drawn over a prohibited ({@code maxOccurs="0"}) element. */
    private void writeCross(int x, int y, int width, int height) {
        write(SEGMENT.formatted(OUTLINE, x, y, x + width - 1, y + height - 1));
        write(SEGMENT.formatted(OUTLINE, x, y + height - 1, x + width - 1, y));
    }

    private void renderRepeated(int x, int y, String name, String rawDocumentation, boolean canExpand, boolean expanded,
                                boolean showsText, Occurrence occurrence, int width, int backgroundWidth) {
        int mainX = x + 3;
        int mainY = y + 3;
        write(SHADOW_RECT.formatted(mainX + SHADOW_OFFSET, mainY + SHADOW_OFFSET, width, HEIGHT));
        write(WHITE_RECT.formatted(position(mainX, mainY), width, HEIGHT));
        write(OUTLINE_RECT.formatted(Shapes.SOLID_STROKE, position(mainX, mainY), width - 1));
        write(WHITE_RECT.formatted(position(x, y), width, HEIGHT));
        writeCheckmark(x + width - 15, y + HEIGHT);
        if (rawDocumentation != null && !rawDocumentation.isEmpty()) {
            writeDocumentation(x, y + 46 - DOC_BG_Y, rawDocumentation);
        }
        write(NAME_BG_RECT.formatted(x + NAME_INSET_X, y + NAME_BG_Y, backgroundWidth));
        writeName(x, y, name);
        if (showsText) {
            writeSimpleContentGlyph(x, y);
        }
        writeOccurrence(x + width, y, occurrence);
        write(OUTLINE_RECT.formatted(Shapes.SOLID_STROKE, position(x, y), width - 1));
        if (canExpand) {
            writeExpandBox(x + width - 6, y + 5, expanded);
        }
    }

    private void writeCheckmark(int x, int y) {
        write(CHECKMARK.formatted(OUTLINE, x, y, x + 6, y + 6, x + 9, y + 3));
    }

    private void writeOccurrence(int baseRight, int y, Occurrence occurrence) {
        OccurrenceIndicator.write(writer, options, baseRight, y, occurrence);
    }

    private void writeDocumentation(int x, int y, String rawDocumentation) {
        Documentation documentation = Documentation.of(rawDocumentation);
        for (int i = 0; i < documentation.lines().size(); i++) {
            Documentation.Line line = documentation.lines().get(i);
            Text.write(writer, options, x + DOC_INSET_X,
                    y + DOC_BASELINE + i * Documentation.LINE_HEIGHT,
                    line.text(), DOC_SIZE, null, null, "gray");
        }
    }

    /** Renders an attribute node: the leaf-element layout plus the attribute icon. */
    public void renderAttribute(int x, int y, String name, String rawDocumentation, boolean icon) {
        int width = NodeGeometry.nodeWidth(name, LEAF_GLYPH_SPACE);
        int backgroundWidth = width - LEAF_GLYPH_SPACE;

        write(SHADOW_RECT.formatted(x + SHADOW_OFFSET, y + SHADOW_OFFSET, width, HEIGHT));
        write(WHITE_RECT.formatted(position(x, y), width, HEIGHT));
        if (rawDocumentation != null && !rawDocumentation.isEmpty()) {
            writeDocumentation(x, y, rawDocumentation);
        }
        write(NAME_BG_RECT.formatted(x + NAME_INSET_X, y + NAME_BG_Y, backgroundWidth));
        writeName(x, y, name);
        if (icon) {
            writeSimpleContentGlyph(x, y);
        }
        write(OUTLINE_RECT.formatted(Shapes.SOLID_STROKE, position(x, y), width - 1));
        flush();
    }

    private void writeName(int x, int y, String name) {
        int colon = name.indexOf(':');
        if (colon <= 0) {
            Text.write(writer, options, x + NAME_INSET_X, y + NAME_BASELINE,
                    name, NAME_SIZE, NAME_WEIGHT, "normal", null);
            return;
        }
        String prefix = name.substring(0, colon + 1);
        String local = name.substring(colon + 1);
        Text.write(writer, options, x + NAME_INSET_X, y + NAME_BASELINE,
                prefix, NAME_SIZE, NAME_WEIGHT, "normal", "gray");
        float prefixWidth = metrics.advanceWidth(prefix, NAME_SIZE, FAMILY, NAME_WEIGHT, "normal");
        Text.write(writer, options, x + NAME_INSET_X + prefixWidth, y + NAME_BASELINE,
                local, NAME_SIZE, NAME_WEIGHT, "normal", null);
    }

    private void writeSimpleContentGlyph(int x, int y) {
        write(SEGMENT.formatted(OUTLINE, x + 2, y + 2, x + 7, y + 2));
        write(SEGMENT.formatted(OUTLINE, x + 2, y + 4, x + 6, y + 4));
        write(SEGMENT.formatted(OUTLINE, x + 2, y + 6, x + 6, y + 6));
    }

    private void writeExpandBox(int boxX, int boxY, boolean expanded) {
        write(EXPAND_BOX.formatted(boxX, boxY, boxX + 1, boxY + 1, boxX + 2, boxY + 5));
        if (!expanded) {
            write(EXPAND_PLUS.formatted(boxX + 5, boxY + 2));
        }
    }

    private static String position(int x, int y) {
        StringBuilder builder = new StringBuilder();
        if (x != 0) {
            builder.append(" x=\"").append(x).append('"');
        }
        if (y != 0) {
            builder.append(" y=\"").append(y).append('"');
        }
        return builder.toString();
    }
}
