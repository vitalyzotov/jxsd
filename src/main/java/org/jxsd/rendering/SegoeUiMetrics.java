package org.jxsd.rendering;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Advance-width metrics for the {@code Segoe UI} text used by the reference
 * diagrams. The per-character advances are recovered from the font in font units
 * and consumed for layout (box and panel sizing, documentation wrapping); the
 * SVG no longer positions individual glyphs, so only the totals matter.
 */
public final class SegoeUiMetrics {

    public static final String FAMILY = "Segoe UI";
    public static final int UNITS_PER_EM = 2048;

    private static final String RESOURCE = "/metrics/segoe-ui-metrics.txt";
    private static final SegoeUiMetrics INSTANCE = new SegoeUiMetrics();
    private static final int DEFAULT_UNITS = 1024;

    private final Map<String, Integer> advances = new HashMap<>();

    private SegoeUiMetrics() {
        load();
    }

    public static SegoeUiMetrics instance() {
        return INSTANCE;
    }

    private static String key(String family, String weight, String style, char character) {
        return family + '\u0000' + weight + '\u0000' + style + '\u0000' + character;
    }

    /** Total advance of {@code text}. */
    public float advanceWidth(String text, float fontSize, String family, String weight, String style) {
        if (text.isEmpty()) {
            return 0f;
        }
        String scope = family + '\u0000' + weight + '\u0000' + style + '\u0000';
        float width = 0f;
        for (int i = 0; i < text.length(); i++) {
            Integer units = advances.get(scope + text.charAt(i));
            if (units == null) {
                units = DEFAULT_UNITS;
            }
            width += units * fontSize / UNITS_PER_EM;
        }
        return width;
    }

    /** C {@code %.8g} formatting: 8 significant digits, half-even, trailing zeros trimmed. */
    public static String formatCoordinate(double value) {
        BigDecimal rounded = new BigDecimal(value, new MathContext(8, RoundingMode.HALF_EVEN)).stripTrailingZeros();
        if (rounded.scale() < 0) {
            rounded = rounded.setScale(0);
        }
        return rounded.toPlainString();
    }

    private void load() {
        try (InputStream in = SegoeUiMetrics.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled resource: " + RESOURCE);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    parseLine(line);
                }
            }
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private void parseLine(String line) {
        if (line.isEmpty() || line.charAt(0) == '#') {
            return;
        }
        String[] parts = line.split("\t", -1);
        if (parts.length < 5 || parts[3].length() != 1) {
            return;
        }
        advances.put(key(parts[0], parts[1], parts[2], parts[3].charAt(0)), Integer.parseInt(parts[4]));
    }
}
