package org.jxsd;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jxsd.cli.Main;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** End-to-end checks of the CLI, driven by the synthetic reference fixtures. */
class CliRenderTest {

    private static final Path LIBRARY = Paths.get("src/test/resources/reference/library.core.v1.xsd");
    private static final Path LOCALIZED = Paths.get("src/test/resources/reference/localized.core.v1.xsd");
    private static final Path EXPAND = Paths.get("tools/xmlspy/xmlspy.expand.v1.xsd");
    private static final String BOOK_DOC = "A book entry.";

    @Test
    void cliExpandRevealsGrandchildren() {
        String one = new String(
                runCli("-s", "-r", "ExpandCompositeType", "-d", "-e", "1", EXPAND.toString()),
                StandardCharsets.UTF_8);
        String two = new String(
                runCli("-s", "-r", "ExpandCompositeType", "-d", "-e", "2", EXPAND.toString()),
                StandardCharsets.UTF_8);
        String three = new String(
                runCli("-s", "-r", "ExpandCompositeType", "-d", "-e", "3", EXPAND.toString()),
                StandardCharsets.UTF_8);
        assertFalse(one.contains("FancyChildType"), "level 1 must not reveal child types");
        assertTrue(two.contains("FancyChildType"), "level 2 must reveal the named child type");
        assertTrue(two.contains("innerNamed"), "level 2 must reveal grandchildren");
        assertFalse(two.contains("leafValue"), "level 2 must not reveal depth 3");
        assertTrue(three.contains("leafValue"), "level 3 must reveal depth 3");
        assertNotEquals(one, two);
        assertNotEquals(two, three);
    }

    @Test
    void cliRendersRootComponentAsSvg() {
        byte[] stdout = runCli("-s", "-r", "Book", LIBRARY.toString());
        String svg = new String(stdout, StandardCharsets.UTF_8);
        assertTrue(svg.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\" ?>"));
        assertTrue(svg.contains("Book"));
        assertTrue(svg.contains("clipPath id=\"cl_2\""));
    }

    @Test
    void cliRendersNamedTypeAsCompositePage() {
        String svg = new String(runCli("-s", "-r", "BookType", "-d", LIBRARY.toString()),
                StandardCharsets.UTF_8);
        assertTrue(svg.contains("BookType"));
        assertTrue(svg.contains("isbn"));
    }

    @Test
    void cliOutputFileIsSvg(@TempDir Path tempDir) throws IOException {
        Path out = tempDir.resolve("cli-out.svg");
        runCli("-o", out.toString(), "-r", "Book", LIBRARY.toString());
        String svg = Files.readString(out, StandardCharsets.UTF_8);
        assertTrue(svg.startsWith("<?xml version=\"1.0\" encoding=\"utf-8\" ?>"));
    }

    @Test
    void cliDocumentationFlagTogglesDocs() {
        String without = new String(runCli("-s", "-r", "Book", LIBRARY.toString()),
                StandardCharsets.UTF_8);
        String with = new String(runCli("-s", "-r", "Book", "-d", LIBRARY.toString()),
                StandardCharsets.UTF_8);
        assertFalse(without.contains(BOOK_DOC));
        assertTrue(with.contains(BOOK_DOC));
    }

    @Test
    void cliLanguageSelectsDocumentation() {
        String english = new String(
                runCli("-s", "-r", "Notice", "-d", "-l", "en", LOCALIZED.toString()),
                StandardCharsets.UTF_8);
        String russian = new String(
                runCli("-s", "-r", "Notice", "-d", "-l", "ru", LOCALIZED.toString()),
                StandardCharsets.UTF_8);
        assertTrue(english.contains("English notice."));
        assertTrue(english.contains("English subject."));
        assertTrue(russian.contains("Русское уведомление."));
        assertTrue(russian.contains("Русская тема."));
    }

    @Test
    void cliAllLanguagesConcatenatesDocumentation() {
        String all = new String(
                runCli("-s", "-r", "Notice", "-d", "--all-languages", LOCALIZED.toString()),
                StandardCharsets.UTF_8);
        assertTrue(all.contains("English notice."), all);
        assertTrue(all.contains("Русское уведомление."), all);
    }

    @Test
    void cliTextLengthOptionPinsTextRuns() {
        String plain = new String(runCli("-s", "-r", "Book", LIBRARY.toString()),
                StandardCharsets.UTF_8);
        String pinned = new String(runCli("-s", "-r", "Book", "--text-length", LIBRARY.toString()),
                StandardCharsets.UTF_8);
        assertFalse(plain.contains("textLength="));
        assertTrue(pinned.contains("textLength="));
        assertTrue(pinned.contains("lengthAdjust=\"spacingAndGlyphs\""));
    }

    @Test
    void cliRendersPngToStdout() {
        byte[] stdout = runCli("-s", "-f", "png", "-r", "Book", "-d", LIBRARY.toString());
        assertPng(stdout);
        assertEquals(283, pngWidth(stdout));
    }

    @Test
    void cliPngSizeMatchesSvg() {
        String svg = new String(runCli("-s", "-r", "Book", "-d", LIBRARY.toString()),
                StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("width=\"(\\d+)\" height=\"(\\d+)\"").matcher(svg);
        assertTrue(matcher.find(), svg);
        int width = Integer.parseInt(matcher.group(1));
        int height = Integer.parseInt(matcher.group(2));

        byte[] png = runCli("-s", "-f", "png", "-r", "Book", "-d", LIBRARY.toString());
        assertEquals(width, pngWidth(png));
        assertEquals(height, pngHeight(png));
    }

    @Test
    void cliPngOutputFile(@TempDir Path tempDir) throws IOException {
        Path out = tempDir.resolve("cli-out.png");
        runCli("-o", out.toString(), "-r", "Book", LIBRARY.toString());
        assertPng(Files.readAllBytes(out));
    }

    @Test
    void cliPngScaleOptionScalesTheRaster() {
        byte[] base = runCli("-s", "-f", "png", "-r", "Book", "-d", LIBRARY.toString());
        byte[] scaled = runCli("-s", "-f", "png", "--scale", "2", "-r", "Book", "-d", LIBRARY.toString());
        assertEquals(pngWidth(base) * 2, pngWidth(scaled));
        assertEquals(pngHeight(base) * 2, pngHeight(scaled));
    }

    @Test
    void cliScaleRequiresPngOutput() {
        byte[] stdout = runCli("-s", "--scale", "2", "-r", "Book", LIBRARY.toString());
        assertEquals(0, stdout.length);
    }

    @Test
    void cliRejectsNonPositiveScale() {
        byte[] stdout = runCli("-s", "-f", "png", "--scale", "0", "-r", "Book", LIBRARY.toString());
        assertEquals(0, stdout.length);
    }

    @Test
    void cliRejectsUnknownOutputFormat() {
        byte[] stdout = runCli("-s", "-f", "tiff", "-r", "Book", LIBRARY.toString());
        assertEquals(0, stdout.length);
    }

    @Test
    void cliHelpDoesNotCrash() {
        String text = new String(runCli("-h"), StandardCharsets.UTF_8);
        assertTrue(text.contains("Usage: jxsd"));
        assertTrue(text.contains("--stdout"));
    }

    @Test
    void cliOutputIsDeterministic() {
        byte[] first = runCli("-s", "-r", "Book", LIBRARY.toString());
        byte[] second = runCli("-s", "-r", "Book", LIBRARY.toString());
        assertArrayEquals(first, second);
    }

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private static void assertPng(byte[] bytes) {
        assertTrue(bytes.length > 24, "not a PNG: " + bytes.length + " bytes");
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            assertEquals(PNG_SIGNATURE[i], bytes[i], "PNG signature byte " + i);
        }
        assertEquals("IHDR", new String(bytes, 12, 4, StandardCharsets.US_ASCII));
    }

    private static int pngWidth(byte[] png) {
        assertPng(png);
        return intAt(png, 16);
    }

    private static int pngHeight(byte[] png) {
        assertPng(png);
        return intAt(png, 20);
    }

    private static int intAt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) << 24 | (bytes[offset + 1] & 0xFF) << 16
                | (bytes[offset + 2] & 0xFF) << 8 | bytes[offset + 3] & 0xFF;
    }

    private static byte[] runCli(String... args) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos, false, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(new ByteArrayOutputStream(), false, StandardCharsets.UTF_8);
        PrintStream savedOut = System.out;
        PrintStream savedErr = System.err;
        try {
            System.setOut(out);
            System.setErr(err);
            Main.run(args);
            out.flush();
        } finally {
            System.setOut(savedOut);
            System.setErr(savedErr);
        }
        return baos.toByteArray();
    }
}
