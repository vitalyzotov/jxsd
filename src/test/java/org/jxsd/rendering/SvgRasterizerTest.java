package org.jxsd.rendering;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** Tests for the pure-Java SVG to PNG rasterizer. */
class SvgRasterizerTest {

    private static final Path GOLDEN = Path.of("src/test/resources/golden/context_book.svg");
    private static final Pattern SIZE = Pattern.compile("width=\"(\\d+)\" height=\"(\\d+)\"");
    private static final byte[] PNG_SIGNATURE =
            {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    @Test
    void rasterizesAtIntrinsicSizeAndIsDeterministic() throws IOException {
        String svg = Files.readString(GOLDEN, StandardCharsets.UTF_8);
        Matcher matcher = SIZE.matcher(svg);
        assertTrue(matcher.find(), svg);

        byte[] png = SvgRasterizer.toPng(svg);
        assertArrayEquals(PNG_SIGNATURE, java.util.Arrays.copyOf(png, 8));
        assertEquals("IHDR", new String(png, 12, 4, StandardCharsets.US_ASCII));
        assertEquals(Integer.parseInt(matcher.group(1)), intAt(png, 16));
        assertEquals(Integer.parseInt(matcher.group(2)), intAt(png, 20));
        assertArrayEquals(png, SvgRasterizer.toPng(svg), "PNG output must be deterministic");
    }

    @Test
    void scalesTheRaster() throws IOException {
        String svg = Files.readString(GOLDEN, StandardCharsets.UTF_8);
        Matcher matcher = SIZE.matcher(svg);
        assertTrue(matcher.find(), svg);
        int width = Integer.parseInt(matcher.group(1));
        int height = Integer.parseInt(matcher.group(2));

        byte[] png = SvgRasterizer.toPng(svg, 2.0);
        assertEquals(width * 2, intAt(png, 16));
        assertEquals(height * 2, intAt(png, 20));
    }

    @Test
    void rejectsNonPositiveScale() throws IOException {
        String svg = Files.readString(GOLDEN, StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> SvgRasterizer.toPng(svg, 0));
        assertThrows(IllegalArgumentException.class, () -> SvgRasterizer.toPng(svg, -1));
        assertThrows(IllegalArgumentException.class, () -> SvgRasterizer.toPng(svg, Double.NaN));
    }

    @Test
    void malformedSvgIsRejected() {
        assertThrows(IOException.class, () -> SvgRasterizer.toPng("<svg><broken"));
    }

    private static int intAt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) << 24 | (bytes[offset + 1] & 0xFF) << 16
                | (bytes[offset + 2] & 0xFF) << 8 | bytes[offset + 3] & 0xFF;
    }
}
