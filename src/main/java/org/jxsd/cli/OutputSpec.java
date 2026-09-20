package org.jxsd.cli;

import java.nio.file.Path;

/**
 * A resolved export target: the format plus either a file or stdout. Resolves
 * the {@code -o}/{@code -s} options together with {@code -f/--format} into a
 * single, validated destination.
 */
record OutputSpec(OutputFormat format, Path file, boolean stdout) {

    /**
     * Resolves the output options. Throws {@link IllegalArgumentException} with
     * a user-facing message when the format or the file extension is invalid.
     */
    static OutputSpec resolve(CliOptions options) {
        OutputFormat explicit = explicitFormat(options.format);

        if (options.outputOnStdOut()) {
            return new OutputSpec(explicit == null ? OutputFormat.SVG : explicit, null, true);
        }

        Path path = Path.of(options.outputFile());
        Path fileNamePath = path.getFileName();
        if (fileNamePath == null) {
            throw new IllegalArgumentException(
                    "The output file must not be a directory: '" + options.outputFile() + "'.");
        }
        String fileName = fileNamePath.toString();
        String extension = extensionOf(fileName);
        OutputFormat inferred = extension == null ? null : OutputFormat.parse(extension).orElse(null);

        OutputFormat format;
        if (explicit != null) {
            if (inferred != null && inferred != explicit) {
                throw new IllegalArgumentException("The output extension '." + extension
                        + "' conflicts with --format " + explicit.extension() + ".");
            }
            format = explicit;
        } else if (extension != null && inferred == null) {
            throw new IllegalArgumentException(
                    "Unknown output extension '." + extension + "' (use '.svg' or '.png').");
        } else {
            format = inferred == null ? OutputFormat.SVG : inferred;
        }

        Path target = extension == null
                ? path.resolveSibling(fileName + "." + format.extension())
                : path;
        return new OutputSpec(format, target, false);
    }

    private static OutputFormat explicitFormat(String token) {
        if (token == null) {
            return null;
        }
        return OutputFormat.parse(token).orElseThrow(() -> new IllegalArgumentException(
                "Unknown output format '" + token + "' (use 'svg' or 'png')."));
    }

    /** The extension after the last dot of a file name, or {@code null}. */
    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(dot + 1) : null;
    }
}
