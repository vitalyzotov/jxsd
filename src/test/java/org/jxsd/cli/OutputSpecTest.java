package org.jxsd.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

/** Tests for the output destination and format resolution. */
class OutputSpecTest {

    private static final String LIB = "http://example.org/library/v1/";

    private static OutputSpec resolve(String... args) {
        return OutputSpec.resolve(parse(args));
    }

    private static CliOptions parse(String... args) {
        CommandLine commandLine = CliOptions.commandLine();
        commandLine.parseArgs(args);
        return commandLine.getCommand();
    }

    private static IllegalArgumentException resolveError(String... args) {
        return assertThrows(IllegalArgumentException.class, () -> resolve(args));
    }

    @Test
    void stdoutDefaultsToSvg() {
        OutputSpec spec = resolve("-s", "-r", "Book", "in.xsd");
        assertEquals(OutputFormat.SVG, spec.format());
        assertTrue(spec.stdout());
    }

    @Test
    void stdoutUsesTheExplicitFormat() {
        assertEquals(OutputFormat.PNG, resolve("-s", "-f", "png", "-r", "Book", "in.xsd").format());
    }

    @Test
    void fileExtensionSelectsTheFormat() {
        assertEquals(OutputFormat.PNG, resolve("-o", "out.png", "-r", "Book", "in.xsd").format());
        assertEquals(OutputFormat.SVG, resolve("-o", "out.SVG", "-r", "Book", "in.xsd").format());
    }

    @Test
    void fileWithoutExtensionGetsSvg() {
        OutputSpec spec = resolve("-o", "diagram", "-r", "Book", "in.xsd");
        assertEquals(Path.of("diagram.svg"), spec.file());
        assertFalse(spec.stdout());
    }

    @Test
    void dotsInDirectoriesAreNotExtensions() {
        OutputSpec withExtension = resolve("-o", "/tmp/a.b/diagram", "-r", "Book", "in.xsd");
        assertEquals(Path.of("/tmp/a.b/diagram.svg"), withExtension.file());
        assertEquals(OutputFormat.SVG, withExtension.format());

        OutputSpec withType = resolve("-o", "/tmp/a.b/diagram.png", "-r", "Book", "in.xsd");
        assertEquals(Path.of("/tmp/a.b/diagram.png"), withType.file());
        assertEquals(OutputFormat.PNG, withType.format());
    }

    @Test
    void unknownExtensionIsRejected() {
        assertTrue(resolveError("-o", "out.tiff", "-r", "Book", "in.xsd").getMessage().contains("Unknown"));
    }

    @Test
    void unknownFormatIsRejected() {
        assertTrue(resolveError("-s", "-f", "tiff", "-r", "Book", "in.xsd").getMessage().contains("Unknown"));
    }

    @Test
    void conflictingFormatAndExtensionAreRejected() {
        assertTrue(resolveError("-o", "out.png", "-f", "svg", "-r", "Book", "in.xsd")
                .getMessage().contains("conflicts"));
    }

    @Test
    void explicitFormatAllowsAnUnusualExtension() {
        OutputSpec spec = resolve("-o", "out.bin", "-f", "png", "-r", "Book", "in.xsd");
        assertEquals(OutputFormat.PNG, spec.format());
        assertEquals(Path.of("out.bin"), spec.file());
    }

    @Test
    void clarkNotationIsAcceptedByTheParser() {
        assertEquals("{" + LIB + "}Book", parse("-o", "out.svg", "-r", "{" + LIB + "}Book", "in.xsd").root);
    }
}
