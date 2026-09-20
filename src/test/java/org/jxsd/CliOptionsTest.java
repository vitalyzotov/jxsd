package org.jxsd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.jxsd.cli.CliOptions;
import org.junit.jupiter.api.Test;

import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

/** Tests for the standard picocli-backed CLI options. */
class CliOptionsTest {

    private static CliOptions parse(String... args) {
        CommandLine commandLine = CliOptions.commandLine();
        commandLine.parseArgs(args);
        return commandLine.getCommand();
    }

    private static ParameterException parseError(String... args) {
        return assertThrows(ParameterException.class, () -> CliOptions.commandLine().parseArgs(args));
    }

    @Test
    void acceptsShortAndLongOutputForms() {
        assertEquals("file.svg", parse("-o", "file.svg", "-r", "A", "in.xsd").outputFile());
        assertEquals("file.svg", parse("-o=file.svg", "-r", "A", "in.xsd").outputFile());
        assertEquals("file.svg", parse("--output", "file.svg", "-r", "A", "in.xsd").outputFile());
        assertEquals("file.svg", parse("--output=file.svg", "-r", "A", "in.xsd").outputFile());
    }

    @Test
    void stdoutIsABooleanFlagAndDoesNotSwallowTheInput() {
        assertTrue(parse("-s", "-r", "A", "in.xsd").outputOnStdOut());
        assertEquals("in.xsd", parse("-s", "-r", "A", "in.xsd").inputFile());
        assertEquals("in.xsd", parse("-s", "in.xsd", "-r", "A").inputFile());
        assertFalse(parse("-o", "out.svg", "-r", "A", "in.xsd").outputOnStdOut());
    }

    @Test
    void outputIsRequiredAndExclusive() {
        parseError("-r", "A", "in.xsd");
        parseError("-o", "out.svg", "-s", "-r", "A", "in.xsd");
    }

    @Test
    void formatIsParsedIndependently() {
        assertEquals("png", parse("-s", "-f", "png", "-r", "A", "in.xsd").format);
        assertEquals("svg", parse("-o", "out.svg", "--format=svg", "-r", "A", "in.xsd").format);
        assertNull(parse("-o", "out.svg", "-r", "A", "in.xsd").format);
    }

    @Test
    void inputFileIsRequiredAndSingle() {
        parseError("-o", "out.svg", "-r", "A");
        parseError("-o", "out.svg", "-r", "A", "a.xsd", "b.xsd");
        assertEquals("in.xsd", parse("-o", "out.svg", "-r", "A", "in.xsd").inputFile());
    }

    @Test
    void unknownOptionsAreRejected() {
        parseError("-o", "out.svg", "-r", "A", "-q", "in.xsd");
    }

    @Test
    void rootIsRequiredAndSingle() {
        parseError("-o", "out.svg", "in.xsd");
        parseError("-o", "out.svg", "-r", "A", "-r", "B", "in.xsd");
        assertEquals("A", parse("-o", "out.svg", "-r", "A", "in.xsd").root);
    }

    @Test
    void namespaceBindingsAccumulate() {
        Map<String, String> bindings = parse(
                "-o", "out.svg", "-r", "tns:A", "-N", "tns=http://a", "-N", "other=http://b", "in.xsd")
                .namespaces;
        assertEquals("http://a", bindings.get("tns"));
        assertEquals("http://b", bindings.get("other"));
    }

    @Test
    void rootKindIsParsed() {
        assertEquals("complexType", parse("-o", "out.svg", "-r", "A", "-k", "complexType", "in.xsd").kind);
        assertNull(parse("-o", "out.svg", "-r", "A", "in.xsd").kind);
    }

    @Test
    void numericOptionsAreValidated() {
        assertEquals(3, parse("-o", "out.svg", "-r", "A", "-e", "3", "in.xsd").expandLevel);
        parseError("-o", "out.svg", "-r", "A", "-e", "x", "in.xsd");
    }

    @Test
    void shortOptionsMayBeClustered() {
        assertTrue(parse("-d", "-o", "out.svg", "-r", "A", "in.xsd").showDocumentation);
        assertEquals(3, parse("-e3", "-o", "out.svg", "-r", "A", "in.xsd").expandLevel);
    }

    @Test
    void languageOption() {
        assertEquals("ru", parse("-o", "out.svg", "-r", "A", "-l", "ru", "in.xsd").language);
        assertEquals("en", parse("-o", "out.svg", "-r", "A", "--language=en", "in.xsd").language);
        assertNull(parse("-o", "out.svg", "-r", "A", "in.xsd").language);
        assertFalse(parse("-o", "out.svg", "-r", "A", "in.xsd").allLanguages);
        assertTrue(parse("-o", "out.svg", "-r", "A", "--all-languages", "in.xsd").allLanguages);
    }

    @Test
    void textLengthOption() {
        assertTrue(parse("-o", "out.svg", "-r", "A", "--text-length", "in.xsd").textLength);
        assertFalse(parse("-o", "out.svg", "-r", "A", "in.xsd").textLength);
    }

    @Test
    void scaleOption() {
        assertEquals(1.0, parse("-o", "out.png", "-r", "A", "in.xsd").scale);
        assertEquals(2.0, parse("-o", "out.png", "-r", "A", "--scale", "2", "in.xsd").scale);
        assertEquals(1.5, parse("-o", "out.png", "-r", "A", "--scale=1.5", "in.xsd").scale);
        parseError("-o", "out.png", "-r", "A", "--scale", "abc", "in.xsd");
    }

    @Test
    void credentials() {
        CliOptions options = parse("-o", "out.svg", "-r", "A", "-u", "bob", "-p", "secret", "in.xsd");
        assertEquals("bob", options.username);
        assertEquals("secret", options.password);
        assertEquals("pass.txt", parse(
                "-o", "out.svg", "-r", "A", "--password-file", "pass.txt", "in.xsd").passwordFile);
        assertNull(parse("-o", "out.svg", "-r", "A", "in.xsd").username);
    }

    @Test
    void helpAndVersionRequests() {
        CommandLine help = CliOptions.commandLine();
        help.parseArgs("-h");
        assertTrue(help.isUsageHelpRequested());

        CommandLine version = CliOptions.commandLine();
        version.parseArgs("--version");
        assertTrue(version.isVersionHelpRequested());
    }

    @Test
    void parsesDoNotLeakState() {
        CliOptions first = parse("-o", "out.svg", "-r", "A", "-u", "bob", "in.xsd");
        CliOptions second = parse("-o", "out.svg", "-r", "B", "in.xsd");
        assertNull(second.username);
        assertEquals("B", second.root);
        assertEquals("bob", first.username);
    }
}
