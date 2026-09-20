package org.jxsd.rendering;

import java.io.Writer;

/** Renders reference-style connector lines between nodes. */
public final class Connector extends SvgWriter {

    private static final String OUTLINE = "fill=\"none\" stroke=\"black\" stroke-width=\"1\" "
            + "stroke-linecap=\"square\" stroke-miterlimit=\"4\" transform=\"translate(0.5 0.5)\"";

    public Connector(Writer writer) {
        super(writer);
    }

    public void line(int x1, int y1, int x2, int y2) {
        write("\t\t<path " + OUTLINE + " d=\"M" + x1 + " " + y1 + "L" + x2 + " " + y2 + "\"/>\n");
        flush();
    }

    /**
     * The dashed horizontal run drawn towards an optional child: the wide junction
     * marker repeated every 6px from {@code startX} up to {@code endX}, all in one
     * path. The reference uses this instead of a solid line so optionality reads as
     * a dashed connector.
     */
    public void dashedHorizontal(int startX, int centerY, int endX) {
        StringBuilder path = new StringBuilder();
        for (int x = startX; x + 2 <= endX; x += 6) {
            path.append(wideMarkerPath(x, centerY));
        }
        write("\t\t<path transform=\"translate(0.5 0.5)\" d=\"" + path + "\"/>\n");
        flush();
    }

    private static String wideMarkerPath(int mx, int centerY) {
        return "M" + SegoeUiMetrics.formatCoordinate(mx) + " " + SegoeUiMetrics.formatCoordinate(centerY - 0.5f)
                + "L" + SegoeUiMetrics.formatCoordinate(mx + 2.5f) + " " + SegoeUiMetrics.formatCoordinate(centerY - 0.5f)
                + "L" + SegoeUiMetrics.formatCoordinate(mx + 2.5f) + " " + SegoeUiMetrics.formatCoordinate(centerY + 0.5f)
                + "L" + SegoeUiMetrics.formatCoordinate(mx - 0.5f) + " " + SegoeUiMetrics.formatCoordinate(centerY + 0.5f)
                + "L" + SegoeUiMetrics.formatCoordinate(mx - 0.5f) + " " + SegoeUiMetrics.formatCoordinate(centerY - 0.5f)
                + "L" + SegoeUiMetrics.formatCoordinate(mx) + " " + SegoeUiMetrics.formatCoordinate(centerY - 0.5f) + "Z";
    }

    /** The narrow junction marker drawn where an optional child meets the branch. */
    public void narrowMarker(int mx, int centerY) {
        String path = "M" + SegoeUiMetrics.formatCoordinate(mx) + " " + SegoeUiMetrics.formatCoordinate(centerY - 0.5f)
                + "L" + scaledRight(mx) + " " + SegoeUiMetrics.formatCoordinate(centerY - 0.5f)
                + "L" + scaledRight(mx) + " " + SegoeUiMetrics.formatCoordinate(centerY + 0.5f)
                + "L" + SegoeUiMetrics.formatCoordinate(mx - 0.5f) + " " + SegoeUiMetrics.formatCoordinate(centerY + 0.5f)
                + "L" + SegoeUiMetrics.formatCoordinate(mx - 0.5f) + " " + SegoeUiMetrics.formatCoordinate(centerY - 0.5f)
                + "L" + SegoeUiMetrics.formatCoordinate(mx) + " " + SegoeUiMetrics.formatCoordinate(centerY - 0.5f) + "Z";
        write("\t\t<path transform=\"translate(0.5 0.5)\" d=\"" + path + "\"/>\n");
        flush();
    }

    /**
     * The reference applies a sub-pixel horizontal scale (about 1.0002436) to the
     * narrow marker's right edge, rounded to three decimals. Reproduced for
     * byte parity.
     */
    private static String scaledRight(int mx) {
        java.math.BigDecimal value = java.math.BigDecimal.valueOf((mx + 0.5) * 1.0002436)
                .setScale(3, java.math.RoundingMode.HALF_UP).stripTrailingZeros();
        if (value.scale() < 0) {
            value = value.setScale(0);
        }
        return value.toPlainString();
    }

    /** Dotted branch used towards an optional child (from {@code fromY} to {@code toY}). */
    public void dashedVertical(int x, int fromY, int toY) {
        if (toY < fromY) {
            dashedVerticalUp(x, fromY, toY);
        } else {
            dashedVerticalDown(x, fromY, toY);
        }
    }

    private void dashedVerticalDown(int x, int fromY, int toY) {
        int last = fromY + 6 * ((toY - fromY - 2) / 6);
        StringBuilder path = new StringBuilder();
        for (int t = fromY; t < last; t += 6) {
            path.append("M").append(SegoeUiMetrics.formatCoordinate(x + 0.5f)).append(' ').append(t)
                    .append("L").append(SegoeUiMetrics.formatCoordinate(x + 0.5f)).append(' ').append(t + 2)
                    .append("L").append(SegoeUiMetrics.formatCoordinate(x + 0.5f)).append(' ')
                    .append(SegoeUiMetrics.formatCoordinate(t + 2.5f))
                    .append("L").append(SegoeUiMetrics.formatCoordinate(x - 0.5f)).append(' ')
                    .append(SegoeUiMetrics.formatCoordinate(t + 2.5f))
                    .append("L").append(SegoeUiMetrics.formatCoordinate(x - 0.5f)).append(' ').append(t + 2)
                    .append("L").append(SegoeUiMetrics.formatCoordinate(x - 0.5f)).append(' ')
                    .append(SegoeUiMetrics.formatCoordinate(t - 0.5f))
                    .append("L").append(SegoeUiMetrics.formatCoordinate(x + 0.5f)).append(' ')
                    .append(SegoeUiMetrics.formatCoordinate(t - 0.5f))
                    .append("L").append(SegoeUiMetrics.formatCoordinate(x + 0.5f)).append(' ').append(t).append('Z');
        }
        float bottom = Math.min(last + 2.5f, toY - 0.5f);
        path.append("M").append(SegoeUiMetrics.formatCoordinate(x + 0.5f)).append(' ').append(last)
                .append("L").append(SegoeUiMetrics.formatCoordinate(x + 0.5f)).append(' ')
                .append(SegoeUiMetrics.formatCoordinate(bottom))
                .append("L").append(SegoeUiMetrics.formatCoordinate(x - 0.5f)).append(' ')
                .append(SegoeUiMetrics.formatCoordinate(bottom))
                .append("L").append(SegoeUiMetrics.formatCoordinate(x - 0.5f)).append(' ')
                .append(SegoeUiMetrics.formatCoordinate(last - 0.5f))
                .append("L").append(SegoeUiMetrics.formatCoordinate(x + 0.5f)).append(' ')
                .append(SegoeUiMetrics.formatCoordinate(last - 0.5f))
                .append("L").append(SegoeUiMetrics.formatCoordinate(x + 0.5f)).append(' ').append(last).append('Z');
        write("\t\t<path transform=\"translate(0.5 0.5)\" d=\"" + path + "\"/>\n");
        flush();
    }

    private void dashedVerticalUp(int x, int fromY, int toY) {
        int last = fromY - 6 * ((fromY - toY - 2) / 6);
        StringBuilder path = new StringBuilder();
        for (int t = fromY; t > last; t -= 6) {
            path.append("M").append(SegoeUiMetrics.formatCoordinate(x - 0.5f)).append(' ').append(t)
                    .append("L").append(SegoeUiMetrics.formatCoordinate(x - 0.5f)).append(' ').append(t - 2)
                    .append("L").append(SegoeUiMetrics.formatCoordinate(x - 0.5f)).append(' ')
                    .append(SegoeUiMetrics.formatCoordinate(t - 2.5f))
                    .append("L").append(SegoeUiMetrics.formatCoordinate(x + 0.5f)).append(' ')
                    .append(SegoeUiMetrics.formatCoordinate(t - 2.5f))
                    .append("L").append(SegoeUiMetrics.formatCoordinate(x + 0.5f)).append(' ').append(t - 2)
                    .append("L").append(SegoeUiMetrics.formatCoordinate(x + 0.5f)).append(' ')
                    .append(SegoeUiMetrics.formatCoordinate(t + 0.5f))
                    .append("L").append(SegoeUiMetrics.formatCoordinate(x - 0.5f)).append(' ')
                    .append(SegoeUiMetrics.formatCoordinate(t + 0.5f))
                    .append("L").append(SegoeUiMetrics.formatCoordinate(x - 0.5f)).append(' ').append(t).append('Z');
        }
        float top = Math.max(last - 2.5f, toY + 0.5f);
        path.append("M").append(SegoeUiMetrics.formatCoordinate(x - 0.5f)).append(' ').append(last)
                .append("L").append(SegoeUiMetrics.formatCoordinate(x - 0.5f)).append(' ')
                .append(SegoeUiMetrics.formatCoordinate(top))
                .append("L").append(SegoeUiMetrics.formatCoordinate(x + 0.5f)).append(' ')
                .append(SegoeUiMetrics.formatCoordinate(top))
                .append("L").append(SegoeUiMetrics.formatCoordinate(x + 0.5f)).append(' ')
                .append(SegoeUiMetrics.formatCoordinate(last + 0.5f))
                .append("L").append(SegoeUiMetrics.formatCoordinate(x - 0.5f)).append(' ')
                .append(SegoeUiMetrics.formatCoordinate(last + 0.5f))
                .append("L").append(SegoeUiMetrics.formatCoordinate(x - 0.5f)).append(' ').append(last).append('Z');
        write("\t\t<path transform=\"translate(0.5 0.5)\" d=\"" + path + "\"/>\n");
        flush();
    }
}
