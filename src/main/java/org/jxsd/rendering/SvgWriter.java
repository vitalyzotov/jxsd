package org.jxsd.rendering;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;

/**
 * Base for the classes that emit SVG fragments through a shared writer. Keeps a
 * non-null writer and wraps every {@link IOException} as an
 * {@link UncheckedIOException}, the convention the renderers rely on.
 */
abstract class SvgWriter {

    protected final Writer writer;

    SvgWriter(Writer writer) {
        if (writer == null) {
            throw new IllegalArgumentException("The writer object is required.");
        }
        this.writer = writer;
    }

    final void write(String text) {
        try {
            writer.write(text);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    final void flush() {
        try {
            writer.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
