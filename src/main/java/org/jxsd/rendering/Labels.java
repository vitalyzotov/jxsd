package org.jxsd.rendering;

/**
 * Truncates node labels the way XMLSpy does: a name whose bold 12px advance
 * exceeds {@link Metrics#LABEL_TRUNCATION_WIDTH} is shortened and
 * suffixed with an ellipsis.
 */
final class Labels {

    private static final String ELLIPSIS = "...";
    private static final float SIZE = 12f;
    private static final String WEIGHT = "600";

    private Labels() {
    }

    static String truncate(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        SegoeUiMetrics metrics = SegoeUiMetrics.instance();
        if (width(metrics, name) <= Metrics.LABEL_TRUNCATION_WIDTH) {
            return name;
        }
        int end = name.length();
        while (end > 0 && width(metrics, name.substring(0, end)) > Metrics.LABEL_TRUNCATION_WIDTH) {
            end--;
        }
        return name.substring(0, end) + ELLIPSIS;
    }

    private static float width(SegoeUiMetrics metrics, String text) {
        return metrics.advanceWidth(text, SIZE, SegoeUiMetrics.FAMILY, WEIGHT, "normal");
    }
}
