package org.jxsd.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Resolves the credentials used for secured XSD dependencies from the command
 * line ({@code -u/-p}), a password file and the environment.
 */
record Credentials(String username, String password) {

    /** Fallback environment variable names. */
    static final String ENV_USERNAME = "JXSD_USERNAME";
    static final String ENV_PASSWORD = "JXSD_PASSWORD";

    static Credentials resolve(CliOptions options) {
        return resolve(options, System::getenv);
    }

    static Credentials resolve(CliOptions options, Function<String, String> environment) {
        if (options.password != null && options.passwordFile != null) {
            throw new IllegalArgumentException(
                    "Use either -p/--password or --password-file, not both.");
        }

        String username = firstNonEmpty(options.username, environment.apply(ENV_USERNAME));
        String password;
        if (options.password != null) {
            password = options.password;
        } else if (options.passwordFile != null) {
            password = readPasswordFile(options.passwordFile);
        } else {
            password = environment.apply(ENV_PASSWORD);
        }
        return new Credentials(username, emptyToNull(password));
    }

    private static String readPasswordFile(String fileName) {
        try {
            String content = Files.readString(Path.of(fileName), StandardCharsets.UTF_8);
            int end = content.length();
            if (end > 0 && content.charAt(end - 1) == '\n') {
                end--;
            }
            if (end > 0 && content.charAt(end - 1) == '\r') {
                end--;
            }
            return content.substring(0, end);
        } catch (IOException ex) {
            throw new IllegalArgumentException(
                    "Cannot read the password file '" + fileName + "': " + ex.getMessage());
        }
    }

    private static String firstNonEmpty(String first, String fallback) {
        if (first != null && !first.isEmpty()) {
            return first;
        }
        return emptyToNull(fallback);
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
