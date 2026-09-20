package org.jxsd.rendering;

import java.io.Writer;
import org.jxsd.model.Occurrence;

/**
 * Renders a reference-style diagram page for a simple-content leaf
 * element: a shadowed box, the bold name, the three-line simple-content glyph
 * and, when present, the gray documentation line. The geometry matches the
 * committed reference pages.
 */
public final class SimpleElementPage extends SvgPage {

    private static final String OUTLINE = "fill=\"none\" stroke=\"black\" stroke-width=\"1\" "
            + "stroke-linecap=\"square\" stroke-miterlimit=\"4\" transform=\"translate(0.5 0.5)\"";

    private static final String FAMILY = SegoeUiMetrics.FAMILY;
    private static final String NAME_WEIGHT = "600";
    private static final String NAME_STYLE = "normal";
    private static final float NAME_SIZE = 12f;
    private static final int NAME_BASELINE = 15;
    private static final int DOC_X = 5;
    private static final int DOC_BG_Y = 27;
    private static final int DOC_BG_HEIGHT = 12;
    private static final int DOC_BASELINE = 37;

    private static final int BOX_HEIGHT = 21;
    private static final int MIN_BOX_WIDTH = 45;
    private static final float BOX_WIDTH_PADDING = 17f;
    private static final int CANVAS_RIGHT_MARGIN = 24;
    private static final int CANVAS_BOTTOM_MARGIN = 14;
    private static final int DOC_RIGHT_MARGIN = 20;
    private static final int DOC_CANVAS_BOTTOM_MARGIN = 10;

    private static final String SILVER_RECT = """
            \t\t<rect fill="silver" x="%s" y="%s" width="%s" height="%s"/>
            """;
    private static final String WHITE_RECT = """
            \t\t<rect fill="white" width="%s" height="%s"/>
            """;
    private static final String NAME_BG_RECT = """
            \t\t<rect fill="white" x="9" y="2" width="%s" height="16"/>
            """;
    private static final String OUTLINE_RECT = """
            \t\t<rect %s width="%s" height="20"/>
            """;
    private static final String PATH = """
            \t\t<path %s d="%s"/>
            """;

    private final SegoeUiMetrics metrics = SegoeUiMetrics.instance();

    public SimpleElementPage(Writer writer) {
        this(writer, RenderOptions.DEFAULT);
    }

    public SimpleElementPage(Writer writer, RenderOptions options) {
        super(writer, options);
    }

    public void render(int pageNumber, String name) {
        render(pageNumber, name, null);
    }

    /**
     * Renders the page for {@code name}; {@code rawDocumentation} is the schema
     * annotation as-is, so runs of whitespace are collapsed only in the emitted
     * text while their advances stay in the pen positions and box width.
     */
    public void render(int pageNumber, String name, String rawDocumentation) {
        render(pageNumber, name, rawDocumentation, Occurrence.SINGLE);
    }

    /**
     * Renders the page for {@code name}. A repeated occurrence is drawn as a base
     * copy plus an offset copy with the {@code min..∞} indicator, exactly like a
     * repeated element inside a chain page.
     */
    public void render(int pageNumber, String name, String rawDocumentation, Occurrence occurrence) {
        if (occurrence.isRepeated()) {
            renderRepeated(pageNumber, name, rawDocumentation, occurrence);
            return;
        }
        boolean hasDocumentation = rawDocumentation != null && !rawDocumentation.isEmpty();
        int boxWidth = boxWidth(name);
        int boxHeight = BOX_HEIGHT;

        Documentation documentation = hasDocumentation
                ? Documentation.of(rawDocumentation) : null;
        int documentationWidth = documentation == null ? 0 : documentation.width();
        int documentationHeight = documentation == null ? 0 : documentation.height(DOC_BG_HEIGHT);

        int canvasWidth = boxWidth + CANVAS_RIGHT_MARGIN;
        int canvasHeight = boxHeight + CANVAS_BOTTOM_MARGIN;
        if (hasDocumentation) {
            canvasWidth = Math.max(canvasWidth, DOC_X + documentationWidth + DOC_RIGHT_MARGIN);
            canvasHeight = DOC_BG_Y + documentationHeight + DOC_CANVAS_BOTTOM_MARGIN;
        }
        open(pageNumber, canvasWidth, canvasHeight);
        write(SILVER_RECT.formatted(4, 4, boxWidth, boxHeight));
        write(WHITE_RECT.formatted(boxWidth, boxHeight));
        if (hasDocumentation) {
            for (int i = 0; i < documentation.lines().size(); i++) {
                Documentation.Line line = documentation.lines().get(i);
                Text.write(writer, options, DOC_X,
                        DOC_BASELINE + i * Documentation.LINE_HEIGHT, line.text(), 9f, null, null, "gray");
            }
        }
        write(NAME_BG_RECT.formatted(boxWidth - 16));
        Text.write(writer, options, 9, NAME_BASELINE, name, NAME_SIZE, NAME_WEIGHT, NAME_STYLE, null);
        write(PATH.formatted(OUTLINE, "M2 2L7 2"));
        write(PATH.formatted(OUTLINE, "M2 4L6 4"));
        write(PATH.formatted(OUTLINE, "M2 6L6 6"));
        write(OUTLINE_RECT.formatted(Shapes.SOLID_STROKE, boxWidth - 1));
        close();
    }

    private void renderRepeated(int pageNumber, String name, String rawDocumentation, Occurrence occurrence) {
        int boxWidth = boxWidth(name);
        boolean hasDocumentation = rawDocumentation != null && !rawDocumentation.isEmpty();
        Documentation documentation = hasDocumentation
                ? Documentation.of(rawDocumentation) : null;
        int documentationHeight = documentation == null ? 0 : documentation.height(DOC_BG_HEIGHT);

        int canvasWidth = boxWidth + 3 + CANVAS_RIGHT_MARGIN;
        if (hasDocumentation) {
            canvasWidth = Math.max(canvasWidth, DOC_X + documentation.width() + DOC_RIGHT_MARGIN);
        }
        int consumed = hasDocumentation ? 46 + documentationHeight : 43;
        int canvasHeight = consumed + 10;

        open(pageNumber, canvasWidth, canvasHeight);
        new ElementNode(writer, options).render(0, 0, name, rawDocumentation, false, false, true,
                occurrence);
        close();
    }

    private int boxWidth(String name) {
        float advance = metrics.advanceWidth(name, NAME_SIZE, FAMILY, NAME_WEIGHT, NAME_STYLE);
        return Math.max(MIN_BOX_WIDTH, (int) Math.ceil(advance + BOX_WIDTH_PADDING));
    }
}
