package org.jxsd.rendering;

import java.util.ArrayList;
import java.util.List;

/** Documentation text lines and the recovered panel size. */
record Documentation(List<Line> lines, int width, int reservedLines, int extraHeight) {

    private static final float SIZE = 9f;
    private static final String WEIGHT = "400";
    private static final String STYLE = "normal";
    private static final float WRAP_WIDTH = Metrics.DOCUMENTATION_WRAP_WIDTH;
    static final int LINE_HEIGHT = 12;

    /** One emitted documentation line. */
    record Line(String text) {
    }

    int height() {
        return reservedLines * LINE_HEIGHT + extraHeight;
    }

    /** Height for the given single-line box height; wrapped annotations stay multi-line. */
    int height(int singleLineHeight) {
        return (reservedLines == 1 ? singleLineHeight : reservedLines * LINE_HEIGHT) + extraHeight;
    }

    /** XML-escapes text content the way the reference generator does. */
    static String escape(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Builds the emitted documentation from the raw annotation the way XMLSpy
     * does: source newlines start a new paragraph, a paragraph's leading
     * whitespace is kept as an indent on its first line, internal runs collapse,
     * and each paragraph word-wraps at {@link Metrics#DOCUMENTATION_WRAP_WIDTH}.
     * The box height counts the emitted lines (empty lines included).
     */
    static Documentation of(String raw) {
        SegoeUiMetrics metrics = SegoeUiMetrics.instance();
        String[] rawLines = raw.split("\n", -1);
        List<Line> lines = new ArrayList<>();
        int width = 0;
        int reserved = 0;
        int extra = 0;
        for (String rawLine : rawLines) {
            if (rawLine.isEmpty()) {
                reserved += 1;
                continue;
            }
            int first = 0;
            while (first < rawLine.length() && Character.isWhitespace(rawLine.charAt(first))) {
                first++;
            }
            if (first == rawLine.length()) {
                extra += 1;
                continue;
            }
            int last = rawLine.length() - 1;
            while (last >= 0 && Character.isWhitespace(rawLine.charAt(last))) {
                last--;
            }
            StringBuilder body = new StringBuilder();
            boolean previousWhitespace = false;
            for (int i = first; i <= last; i++) {
                char current = rawLine.charAt(i);
                boolean whitespace = Character.isWhitespace(current);
                if (whitespace && previousWhitespace) {
                    continue;
                }
                previousWhitespace = whitespace;
                body.append(whitespace ? ' ' : current);
            }
            String indent = " ".repeat(first);
            List<String> wrapped = wrap(metrics, body.toString(),
                    metrics.advanceWidth(indent, SIZE, SegoeUiMetrics.FAMILY, WEIGHT, STYLE));
            boolean firstLine = true;
            for (String chunk : wrapped) {
                String line = firstLine ? indent + chunk : chunk;
                firstLine = false;
                float advance = metrics.advanceWidth(line, SIZE, SegoeUiMetrics.FAMILY, WEIGHT, STYLE);
                width = Math.max(width, (int) Math.ceil(advance + 1f));
                lines.add(new Line(line));
                reserved += 1;
            }
        }
        if (lines.isEmpty()) {
            lines.add(new Line(""));
        }
        return new Documentation(List.copyOf(lines), width, reserved, extra);
    }

    /** Greedy word wrap; {@code firstIndent} reduces the first line's budget. */
    private static List<String> wrap(SegoeUiMetrics metrics, String body, float firstIndent) {
        List<String> result = new ArrayList<>();
        float space = metrics.advanceWidth(" ", SIZE, SegoeUiMetrics.FAMILY, WEIGHT, STYLE);
        float limit = WRAP_WIDTH - firstIndent;
        StringBuilder current = new StringBuilder();
        float currentWidth = 0f;
        for (String word : body.split(" ")) {
            float wordWidth = metrics.advanceWidth(word, SIZE, SegoeUiMetrics.FAMILY, WEIGHT, STYLE);
            if (current.length() == 0) {
                current.append(word);
                currentWidth = wordWidth;
            } else if (currentWidth + space + wordWidth <= limit) {
                current.append(' ').append(word);
                currentWidth += space + wordWidth;
            } else {
                result.add(current.toString());
                current.setLength(0);
                current.append(word);
                currentWidth = wordWidth;
                limit = WRAP_WIDTH;
            }
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        if (result.isEmpty()) {
            result.add("");
        }
        return result;
    }
}
