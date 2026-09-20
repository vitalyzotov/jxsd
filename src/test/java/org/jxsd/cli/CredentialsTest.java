package org.jxsd.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import picocli.CommandLine;

/** Tests for credential resolution (command line, password file, environment). */
class CredentialsTest {

    private static CliOptions parse(String... args) {
        CommandLine commandLine = CliOptions.commandLine();
        commandLine.parseArgs(args);
        return commandLine.getCommand();
    }

    private static Function<String, String> environment(Map<String, String> values) {
        return values::get;
    }

    @Test
    void commandLineCredentialsAreUsed() {
        Credentials credentials = Credentials.resolve(
                parse("-s", "-r", "A", "-u", "bob", "-p", "secret", "in.xsd"), environment(Map.of()));
        assertEquals("bob", credentials.username());
        assertEquals("secret", credentials.password());
    }

    @Test
    void passwordFileTrailingNewlineIsStripped(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("secret.txt");
        Files.writeString(file, "s3cret\r\n", StandardCharsets.UTF_8);
        Credentials credentials = Credentials.resolve(
                parse("-s", "-r", "A", "-u", "bob", "--password-file", file.toString(), "in.xsd"),
                environment(Map.of()));
        assertEquals("s3cret", credentials.password());
    }

    @Test
    void environmentIsTheFallback() {
        Credentials credentials = Credentials.resolve(parse("-s", "-r", "A", "in.xsd"),
                environment(Map.of(Credentials.ENV_USERNAME, "envuser", Credentials.ENV_PASSWORD, "envpass")));
        assertEquals("envuser", credentials.username());
        assertEquals("envpass", credentials.password());
    }

    @Test
    void commandLineOverridesTheEnvironment() {
        Credentials credentials = Credentials.resolve(parse("-s", "-r", "A", "-u", "cli", "in.xsd"),
                environment(Map.of(Credentials.ENV_USERNAME, "envuser", Credentials.ENV_PASSWORD, "envpass")));
        assertEquals("cli", credentials.username());
        assertEquals("envpass", credentials.password());
    }

    @Test
    void passwordAndPasswordFileAreMutuallyExclusive() {
        assertThrows(IllegalArgumentException.class, () -> Credentials.resolve(
                parse("-s", "-r", "A", "-p", "x", "--password-file", "f", "in.xsd"), environment(Map.of())));
    }

    @Test
    void missingPasswordFileIsReported() {
        assertThrows(IllegalArgumentException.class, () -> Credentials.resolve(
                parse("-s", "-r", "A", "--password-file", "/no/such/file", "in.xsd"), environment(Map.of())));
    }
}
