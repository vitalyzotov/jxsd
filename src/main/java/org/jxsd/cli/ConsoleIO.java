package org.jxsd.cli;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Console output helpers. Informational progress is suppressed while the
 * diagram is streamed to stdout ({@code -s/--stdout}); warnings and errors
 * always go to stderr.
 */
final class ConsoleIO {

    private final boolean quiet;

    ConsoleIO(boolean quiet) {
        this.quiet = quiet;
    }

    /** A progress message on stdout, suppressed for {@code -s/--stdout}. */
    void info(String message) {
        if (!quiet) {
            System.out.println(message);
        }
    }

    /** A warning on stderr; the command can still succeed. */
    void warn(String message) {
        System.err.println(message);
    }

    /** An error on stderr. */
    void error(String message) {
        System.err.println(message);
    }

    /** Wraps an OutputStream so that close() is a no-op (keeps System.out open after export). */
    static final class UncloseableOutputStream extends OutputStream {

        private final OutputStream inner;

        UncloseableOutputStream(OutputStream inner) {
            this.inner = inner;
        }

        @Override
        public void write(int b) throws IOException { inner.write(b); }

        @Override
        public void write(byte[] b, int off, int len) throws IOException { inner.write(b, off, len); }

        @Override
        public void flush() throws IOException { inner.flush(); }

        @Override
        public void close() { /* no-op */ }
    }
}
