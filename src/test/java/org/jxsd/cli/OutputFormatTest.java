package org.jxsd.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests for the {@link OutputFormat} token resolution. */
class OutputFormatTest {

    @Test
    void resolvesKnownTokensCaseInsensitively() {
        assertEquals(OutputFormat.SVG, OutputFormat.parse(null).orElseThrow());
        assertEquals(OutputFormat.SVG, OutputFormat.parse("svg").orElseThrow());
        assertEquals(OutputFormat.SVG, OutputFormat.parse("SVG").orElseThrow());
        assertEquals(OutputFormat.PNG, OutputFormat.parse("png").orElseThrow());
        assertEquals(OutputFormat.PNG, OutputFormat.parse("PNG").orElseThrow());
    }

    @Test
    void rejectsEmptyAndUnknownTokens() {
        assertTrue(OutputFormat.parse("").isEmpty());
        assertTrue(OutputFormat.parse("tiff").isEmpty());
        assertTrue(OutputFormat.parse("svg2").isEmpty());
    }

    @Test
    void extensionsAreLowerCase() {
        assertEquals("svg", OutputFormat.SVG.extension());
        assertEquals("png", OutputFormat.PNG.extension());
    }
}
