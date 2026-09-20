package org.jxsd.rendering;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;

/**
 * Emits a single SVG text run. Glyphs are laid out by the viewer (one start
 * {@code x}); the recovered metrics only size the surrounding boxes and panels.
 * With {@link RenderOptions#textLength()} the run is pinned to the computed
 * width so a substituted font cannot change the layout.
 */
final class Text {

    private static final String FAMILY = SegoeUiMetrics.FAMILY;

    private Text() {
    }

    static void write(Writer writer, RenderOptions options, float x, float baselineY,
                      String text, float size, String weight, String style, String fill) {
        write(writer, options, x, baselineY, text, size, weight, style, fill, null);
    }

    static void write(Writer writer, RenderOptions options, float x, float baselineY,
                      String text, float size, String weight, String style, String fill, String anchor) {
        write(writer, options, x, baselineY, text, size, weight, style, fill, anchor, FAMILY);
    }

    static void write(Writer writer, RenderOptions options, float x, float baselineY,
                      String text, float size, String weight, String style, String fill, String anchor, String family) {
        String actualWeight = weight == null ? "400" : weight;
        String actualStyle = style == null ? "normal" : style;
        String actualFamily = family == null ? FAMILY : family;
        StringBuilder tag = new StringBuilder("\t\t<text");
        if (fill != null) {
            tag.append(" fill=\"").append(fill).append('"');
        }
        tag.append(" font-size=\"").append(SegoeUiMetrics.formatCoordinate(size)).append('"');
        if (!"400".equals(actualWeight)) {
            tag.append(" font-weight=\"").append(actualWeight).append('"');
        }
        if (!"normal".equals(actualStyle)) {
            tag.append(" font-style=\"").append(actualStyle).append('"');
        }
        tag.append(" font-family=\"").append(actualFamily).append('"');
        tag.append(" x=\"").append(SegoeUiMetrics.formatCoordinate(x)).append('"');
        tag.append(" y=\"").append(SegoeUiMetrics.formatCoordinate(baselineY)).append('"');
        if (anchor != null) {
            tag.append(" text-anchor=\"").append(anchor).append('"');
        }
        if (options.textLength()) {
            float width = SegoeUiMetrics.instance().advanceWidth(text, size, actualFamily, actualWeight, actualStyle);
            tag.append(" textLength=\"").append(SegoeUiMetrics.formatCoordinate(width))
                    .append("\" lengthAdjust=\"spacingAndGlyphs\"");
        }
        tag.append(">\n");
        tag.append("\t\t\t\t").append(Documentation.escape(text)).append('\n');
        tag.append("\t\t</text>\n");
        writeRaw(writer, tag.toString());
    }

    private static void writeRaw(Writer writer, String text) {
        try {
            writer.write(text);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
