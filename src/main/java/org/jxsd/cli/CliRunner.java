package org.jxsd.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.jxsd.parsing.Schema;
import org.jxsd.parsing.SchemaComponent;
import org.jxsd.rendering.PageRenderer;
import org.jxsd.rendering.RenderOptions;
import org.jxsd.rendering.SvgRasterizer;
import org.jxsd.model.Diagram;

import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

/** Runs the console-mode pipeline: parse, load, build the diagram, export. */
final class CliRunner {

    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_FAILURE = 1;

    /**
     * Runs the CLI and returns the process exit code. {@code Main} owns the
     * process, so the code is returned rather than passed to {@code System.exit}.
     */
    int run(String[] args) {
        CommandLine commandLine = CliOptions.commandLine();
        try {
            commandLine.parseArgs(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            commandLine.usage(System.err);
            return EXIT_FAILURE;
        }

        if (commandLine.isUsageHelpRequested()) {
            commandLine.usage(System.out);
            return EXIT_SUCCESS;
        }
        if (commandLine.isVersionHelpRequested()) {
            commandLine.printVersionHelp(System.out);
            return EXIT_SUCCESS;
        }

        CliOptions options = commandLine.getCommand();
        ConsoleIO io = new ConsoleIO(options.outputOnStdOut());

        if (options.expandLevel < 0) {
            io.error("The --expand option must not be negative.");
            return EXIT_FAILURE;
        }
        if (options.language != null && options.language.isEmpty()) {
            io.error("The --language option must not be empty; "
                    + "use --all-languages to concatenate every entry.");
            return EXIT_FAILURE;
        }
        if (options.allLanguages && options.language != null) {
            io.error("Use either -l/--language or --all-languages, not both.");
            return EXIT_FAILURE;
        }

        OutputSpec output;
        Credentials credentials;
        try {
            output = OutputSpec.resolve(options);
            credentials = Credentials.resolve(options);
        } catch (IllegalArgumentException ex) {
            io.error(ex.getMessage());
            return EXIT_FAILURE;
        }

        if (!(options.scale > 0) || Double.isInfinite(options.scale)) {
            io.error("The --scale option must be a positive number.");
            return EXIT_FAILURE;
        }
        if (output.format() != OutputFormat.PNG && options.scale != 1.0) {
            io.error("The --scale option requires PNG output.");
            return EXIT_FAILURE;
        }
        if (credentials.password() != null
                && (credentials.username() == null || credentials.username().isEmpty())) {
            io.warn("Ignoring the password: no username was given (use -u/--username).");
        }

        io.info("Loading " + options.inputFile());

        Schema schema = new Schema();
        if (credentials.username() != null && !credentials.username().isEmpty()) {
            schema.setCredentials(credentials.username(), credentials.password());
        }
        schema.load(options.inputFile(), io::error);

        Diagram diagram = new Diagram();
        diagram.setShowDocumentation(options.showDocumentation);
        diagram.setSchema(schema);
        diagram.setLanguage(resolveLanguage(options));

        SchemaComponent root;
        try {
            root = RootSelector.select(schema, options.root, options.kind, options.namespaces);
        } catch (IllegalArgumentException ex) {
            io.error(ex.getMessage());
            return EXIT_FAILURE;
        }
        io.info("Root component: " + options.root);
        diagram.addRoot(root.tag(), root.namespace());
        diagram.expand(options.expandLevel);

        return exportReference(io, options, output, diagram);
    }

    /** {@code null} selects the default language, an empty string concatenates all. */
    private static String resolveLanguage(CliOptions options) {
        return options.allLanguages ? "" : options.language;
    }

    private static int exportReference(ConsoleIO io, CliOptions options, OutputSpec output, Diagram diagram) {
        // PNG is not editable text, so always pin text runs to their measured
        // width: the raster must keep the metric-computed layout even when the
        // reference font is unavailable.
        boolean pinText = options.textLength || output.format() == OutputFormat.PNG;
        Optional<String> rendered = new PageRenderer(diagram.context(), new RenderOptions(pinText))
                .render(0, diagram.getRootElements().getFirst());
        if (rendered.isEmpty()) {
            io.error("The component '" + options.root + "' has no supported diagram shape.");
            return EXIT_FAILURE;
        }
        String svg = rendered.get();

        try {
            byte[] bytes = output.format() == OutputFormat.PNG
                    ? SvgRasterizer.toPng(svg, options.scale)
                    : svg.getBytes(StandardCharsets.UTF_8);
            if (output.stdout()) {
                OutputStream stream = new ConsoleIO.UncloseableOutputStream(System.out);
                stream.write(bytes);
                stream.flush();
            } else {
                createParentDirectories(output.file());
                Files.write(output.file(), bytes);
                io.info("Wrote " + output.file());
            }
            return EXIT_SUCCESS;
        } catch (IOException ex) {
            io.error("Unable to write " + output.file() + ": " + ex.getMessage());
            return EXIT_FAILURE;
        }
    }

    private static void createParentDirectories(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
