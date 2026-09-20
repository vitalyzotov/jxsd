package org.jxsd.rendering;

import java.io.Writer;
import org.jxsd.model.Occurrence;

/**
 * Renders the bottom-right occurrence indicator the way XMLSpy does: the
 * minimum, the {@code ..} separator and the maximum are three separate runs,
 * the group is inset a few pixels from the node's right edge, and an unbounded
 * maximum uses Segoe UI's infinity glyph ({@code \u221e}), which renders on every
 * platform (unlike XMLSpy's {@code Symbol}/{@code ¥} convention).
 */
final class OccurrenceIndicator {

    private static final float SIZE = 12f;
    private static final int RIGHT_INSET = 8;
    private static final int BASELINE_FROM_TOP = 40;
    private static final String UNBOUNDED_GLYPH = "\u221e";

    private OccurrenceIndicator() {
    }

    static void write(Writer writer, RenderOptions options, float nodeRight, float nodeTop, Occurrence occurrence) {
        SegoeUiMetrics metrics = SegoeUiMetrics.instance();
        String minText = Integer.toString(occurrence.min());
        String separator = "..";
        String maxText = occurrence.isUnbounded() ? UNBOUNDED_GLYPH : Integer.toString(occurrence.max());

        float minWidth = metrics.advanceWidth(minText, SIZE, SegoeUiMetrics.FAMILY, "400", "normal");
        float separatorWidth = metrics.advanceWidth(separator, SIZE, SegoeUiMetrics.FAMILY, "400", "normal");
        float maxWidth = metrics.advanceWidth(maxText, SIZE, SegoeUiMetrics.FAMILY, "400", "normal");
        float start = nodeRight - RIGHT_INSET - (minWidth + separatorWidth + maxWidth);
        float baseline = nodeTop + BASELINE_FROM_TOP;

        Text.write(writer, options, start, baseline, minText, SIZE, null, null, null);
        Text.write(writer, options, start + minWidth, baseline, separator, SIZE, null, null, null);
        Text.write(writer, options, start + minWidth + separatorWidth, baseline,
                maxText, SIZE, null, null, null);
    }
}
