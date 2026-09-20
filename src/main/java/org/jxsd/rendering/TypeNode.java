package org.jxsd.rendering;

import java.io.Writer;

/**
 * Renders a reference-style complex-type node: a left-beveled
 * hexagon with a shadow, the bold name and an optional gray documentation.
 * Geometry is recovered from the reference pages.
 */
public final class TypeNode extends SvgPage {

    private static final String OUTLINE = "fill=\"none\" stroke=\"black\" stroke-width=\"1\" "
            + "stroke-linecap=\"square\" stroke-miterlimit=\"4\" transform=\"translate(0.5 0.5)\"";

    private static final String FAMILY = SegoeUiMetrics.FAMILY;
    private static final String NAME_WEIGHT = "600";
    private static final float NAME_SIZE = 12f;
    private static final float DOC_SIZE = 9f;
    private static final int NAME_INSET_X = 5;
    private static final int NAME_BG_Y = 2;
    private static final int NAME_BASELINE = 15;
    private static final int DOC_INSET_X = 5;
    private static final int DOC_BASELINE = 37;
    private static final int SHADOW_OFFSET = 4;
    private static final int TYPE_HEIGHT = 22;
    private static final float TYPE_WIDTH_PADDING = 19f;

    private static final String FILL_PATH = """
            \t\t<path fill="%s" d="%s"/>
            """;
    private static final String OUTLINE_PATH = """
            \t\t<path %s d="%s"/>
            """;
    private static final String NAME_BG_RECT = """
            \t\t<rect fill="white" x="%s" y="%s" width="%s" height="16"/>
            """;

    private final SegoeUiMetrics metrics = SegoeUiMetrics.instance();

    public TypeNode(Writer writer, RenderOptions options) {
        super(writer, options);
    }

    public void render(int x, int y, String name, String rawDocumentation) {
        int width = width(name);
        int height = TYPE_HEIGHT;
        String fill = Shapes.path(Shapes.ShapeKind.TYPE, x, y, width, height);
        String shadow = Shapes.path(Shapes.ShapeKind.TYPE,
                x + SHADOW_OFFSET, y + SHADOW_OFFSET, width, height);
        String outline = Shapes.outline(Shapes.ShapeKind.TYPE, x, y, width, height);

        write(FILL_PATH.formatted("silver", shadow));
        write(FILL_PATH.formatted("white", fill));
        if (rawDocumentation != null && !rawDocumentation.isEmpty()) {
            Documentation documentation = Documentation.of(rawDocumentation);
            for (int i = 0; i < documentation.lines().size(); i++) {
                Documentation.Line line = documentation.lines().get(i);
                Text.write(writer, options, x + DOC_INSET_X,
                        y + DOC_BASELINE + i * Documentation.LINE_HEIGHT,
                        line.text(), DOC_SIZE, null, null, "gray");
            }
        }
        write(NAME_BG_RECT.formatted(x + NAME_INSET_X, y + NAME_BG_Y, width - 18));
        Text.write(writer, options, x + NAME_INSET_X, y + NAME_BASELINE,
                name, NAME_SIZE, NAME_WEIGHT, "normal", null);
        write(OUTLINE_PATH.formatted(OUTLINE, outline));
        flush();
    }

    private int width(String name) {
        return widthFor(name);
    }

    /** Width of the type hexagon for {@code name}, used to size lone type pages. */
    static int widthFor(String name) {
        float advance = SegoeUiMetrics.instance().advanceWidth(name, NAME_SIZE, FAMILY, NAME_WEIGHT, "normal");
        return (int) Math.ceil(advance + TYPE_WIDTH_PADDING);
    }
}
