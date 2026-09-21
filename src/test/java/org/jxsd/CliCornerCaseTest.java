package org.jxsd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jxsd.cli.Main;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Error paths and option handling of the CLI. */
class CliCornerCaseTest {

    private static final String LIBRARY = "src/test/resources/reference/library.core.v1.xsd";
    private static final String EDGE = "src/test/resources/reference/library.edge.v1.xsd";
    private static final String DOCTYPE_FIXTURE =
            "src/test/resources/reference/insecure-doctype.core.v1.xsd";
    private static final String MISSING = "src/test/resources/reference/missing-dependency.core.v1.xsd";
    private static final String MISSING_MULTIPLE =
            "src/test/resources/reference/missing-multiple.core.v1.xsd";
    private static final String LIB_NS = "http://example.org/library/v1/";

    @Test
    void parseErrorDoesNotRender() {
        Run run = run("-s");
        assertEquals(1, run.code());
        assertEquals(0, run.out().length());
        assertFalse(run.err().isEmpty());
    }

    @Test
    void conflictingOutputGroupIsRejected(@TempDir Path tempDir) {
        Run run = run("-o", tempDir.resolve("out.svg").toString(), "-s", "-r", "Book", LIBRARY);
        assertEquals(1, run.code());
        assertEquals(0, run.out().length());
        assertFalse(run.err().isEmpty());
    }

    @Test
    void versionRequestPrintsVersion() {
        Run run = run("-V");
        assertEquals(0, run.code());
        assertTrue(run.out().contains("jxsd, version"));
    }

    @Test
    void helpRequestPrintsUsage() {
        Run run = run("-h");
        assertEquals(0, run.code());
        assertTrue(run.out().contains("Usage: jxsd"));
    }

    @Test
    void negativeExpandLevelIsRejected() {
        Run run = run("-o", "target/never-written.svg", "-r", "Book", "-e", "-1", LIBRARY);
        assertEquals(1, run.code());
        assertEquals(0, run.out().length());
        assertTrue(run.err().contains("must not be negative"), run.err());
    }

    @Test
    void repeatedRootIsRejected() {
        Run run = run("-s", "-r", "Book", "-r", "Catalog", LIBRARY);
        assertEquals(1, run.code());
        assertEquals(0, run.out().length());
    }

    @Test
    void missingRootIsRejected() {
        Run run = run("-s", LIBRARY);
        assertEquals(1, run.code());
        assertEquals(0, run.out().length());
    }

    @Test
    void unknownRootIsReported() {
        Run run = run("-o", "target/never-written.svg", "-r", "Nope", LIBRARY);
        assertEquals(1, run.code());
        assertTrue(run.err().contains("not found"), run.err());
    }

    @Test
    void unknownPrefixIsReported() {
        Run run = run("-s", "-r", "nope:Book", LIBRARY);
        assertEquals(1, run.code());
        assertTrue(run.err().contains("Unknown namespace prefix"), run.err());
    }

    @Test
    void clarkNotationRootRenders() {
        Run run = run("-s", "-r", "{" + LIB_NS + "}Book", LIBRARY);
        assertEquals(0, run.code(), run.err());
        assertTrue(run.out().startsWith("<?xml"));
    }

    @Test
    void unsupportedShapesAreReported() {
        Run group = run("-s", "-r", "NameGroup", LIBRARY);
        assertEquals(1, group.code());
        assertTrue(group.err().contains("no supported diagram shape"), group.err());
        assertTrue(run("-s", "-r", "NestedWithAttrsType", EDGE).err().contains("no supported diagram shape"));
    }

    @Test
    void outputWithoutExtensionGetsSvg(@TempDir Path tempDir) throws IOException {
        Path out = tempDir.resolve("diagram");
        assertEquals(0, run("-o", out.toString(), "-r", "Book", LIBRARY).code());

        Path withExtension = tempDir.resolve("diagram.svg");
        assertTrue(Files.exists(withExtension));
        assertTrue(Files.readString(withExtension, StandardCharsets.UTF_8).startsWith("<?xml"));
    }

    @Test
    void outputCreatesMissingDirectories(@TempDir Path tempDir) throws IOException {
        Path out = tempDir.resolve("nested/deeper/diagram.svg");
        assertEquals(0, run("-o", out.toString(), "-r", "Book", LIBRARY).code());
        assertTrue(Files.exists(out));
    }

    @Test
    void dotInDirectoryNameIsNotAnExtension(@TempDir Path tempDir) throws IOException {
        Path out = tempDir.resolve("v1.2/diagram");
        assertEquals(0, run("-o", out.toString(), "-r", "Book", LIBRARY).code());
        assertTrue(Files.exists(tempDir.resolve("v1.2/diagram.svg")));
    }

    @Test
    void nonSupportedOutputFileExtensionIsRejected(@TempDir Path tempDir) throws IOException {
        Path out = tempDir.resolve("diagram.txt");
        Run run = run("-o", out.toString(), "-r", "Book", LIBRARY);

        assertEquals(1, run.code());
        assertTrue(run.err().contains("Unknown output extension"), run.err());
        assertFalse(Files.exists(out));
    }

    @Test
    void conflictingFormatAndExtensionAreRejected(@TempDir Path tempDir) {
        Run run = run("-o", tempDir.resolve("diagram.svg").toString(), "-f", "png", "-r", "Book", LIBRARY);
        assertEquals(1, run.code());
        assertTrue(run.err().contains("conflicts"), run.err());
    }

    @Test
    void unknownFormatIsRejected() {
        Run run = run("-s", "-f", "tiff", "-r", "Book", LIBRARY);
        assertEquals(1, run.code());
        assertTrue(run.err().contains("Unknown output format"), run.err());
    }

    @Test
    void pngOutputFileIsAccepted(@TempDir Path tempDir) throws IOException {
        Path out = tempDir.resolve("diagram.png");
        Run run = run("-o", out.toString(), "-r", "Book", LIBRARY);

        assertEquals(0, run.code(), run.err());
        assertTrue(Files.exists(out));
        byte[] bytes = Files.readAllBytes(out);
        assertEquals((byte) 0x89, bytes[0]);
        assertEquals('P', bytes[1]);
        assertEquals('N', bytes[2]);
        assertEquals('G', bytes[3]);
    }

    @Test
    void credentialsAreAccepted(@TempDir Path tempDir) throws IOException {
        Path out = tempDir.resolve("secured.svg");
        run("-o", out.toString(), "-u", "bob", "-p", "secret", "-r", "Book", LIBRARY);
        assertTrue(Files.exists(out));
    }

    @Test
    void passwordWithoutUsernameIsWarnedAbout() {
        Run run = run("-s", "-p", "secret", "-r", "Book", LIBRARY);
        assertEquals(0, run.code(), run.err());
        assertTrue(run.err().contains("no username"), run.err());
    }

    @Test
    void emptyLanguageIsRejected() {
        Run run = run("-s", "-l", "", "-r", "Book", LIBRARY);
        assertEquals(1, run.code());
        assertTrue(run.err().contains("must not be empty"), run.err());
    }

    @Test
    void languageAndAllLanguagesAreMutuallyExclusive() {
        Run run = run("-s", "-l", "en", "--all-languages", "-r", "Notice",
                "src/test/resources/reference/localized.core.v1.xsd");
        assertEquals(1, run.code());
        assertTrue(run.err().contains("not both"), run.err());
    }

    @Test
    void loadErrorsDoNotStopTheRender() {
        Run run = run("-s", "-r", "Standalone", MISSING);
        assertTrue(run.err().contains("Dependency not found"), run.err());
        assertTrue(run.out().contains("Standalone"));
    }

    @Test
    void multipleLoadErrorsGoToSeparateLines() {
        Run run = run("-s", "-r", "Standalone", MISSING_MULTIPLE);
        List<String> lines = run.err().lines().toList();
        assertTrue(lines.stream().anyMatch(line -> line.contains("first-missing")), run.err());
        assertTrue(lines.stream().anyMatch(line -> line.contains("second-missing")), run.err());
        assertFalse(lines.stream().anyMatch(line -> line.contains("first-missing")
                && line.contains("second-missing")), "errors must not share a line:\n" + run.err());
    }

    @Test
    void expandingBeyondTheTreeDepthStillRenders() {
        Run run = run("-s", "-r", "Comment", "-e", "5", LIBRARY);
        assertEquals(0, run.code(), run.err());
        assertTrue(run.out().startsWith("<?xml"), run.out());
    }

    @Test
    void doctypeIsRejectedByDefault() {
        Run run = run("-s", "-r", "Standalone", DOCTYPE_FIXTURE);
        assertEquals(1, run.code());
        assertTrue(run.err().contains("DOCTYPE"), run.err());
    }

    @Test
    void insecureAllowsDoctypeAndIsWarnedAbout() {
        Run run = run("-s", "--insecure", "-r", "Standalone", DOCTYPE_FIXTURE);
        assertEquals(0, run.code(), run.err());
        assertTrue(run.err().contains("--insecure"), run.err());
        assertTrue(run.out().startsWith("<?xml"), run.out());
    }

    private record Run(int code, byte[] bytes, String err) {
        String out() {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private static Run run(String... args) {
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(stdout, true, StandardCharsets.UTF_8);
        PrintStream err = new PrintStream(stderr, true, StandardCharsets.UTF_8);
        PrintStream savedOut = System.out;
        PrintStream savedErr = System.err;
        int code;
        try {
            System.setOut(out);
            System.setErr(err);
            code = Main.run(args);
        } finally {
            System.setOut(savedOut);
            System.setErr(savedErr);
        }
        return new Run(code, stdout.toByteArray(), stderr.toString(StandardCharsets.UTF_8));
    }
}
