package org.jxsd.cli;

/** CLI entry point. */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        System.exit(run(args));
    }

    /** Runs the CLI in-process and returns the exit code (used by tests). */
    public static int run(String[] args) {
        return new CliRunner().run(args);
    }
}
