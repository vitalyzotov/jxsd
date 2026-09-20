package org.jxsd.cli;

import java.util.Locale;
import java.util.Optional;

/** Supported diagram export formats for {@code -o} and {@code -s}. */
enum OutputFormat {

    SVG,
    PNG;

    /** The lower-case extension (and {@code -s} token) of this format. */
    String extension() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Resolves a file extension or {@code -s} token; {@code null} defaults to
     * {@link #SVG}. Empty or unknown tokens return an empty result.
     */
    static Optional<OutputFormat> parse(String token) {
        if (token == null) {
            return Optional.of(SVG);
        }
        return switch (token.toLowerCase(Locale.ROOT)) {
            case "svg" -> Optional.of(SVG);
            case "png" -> Optional.of(PNG);
            default -> Optional.empty();
        };
    }
}
