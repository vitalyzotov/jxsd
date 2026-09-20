package org.jxsd.rendering;

import java.io.Writer;

/**
 * Base for the reference page renderers: shared {@link RenderOptions}, writer
 * access and the SVG document chrome (clip path, white backdrop) that every
 * page opens and closes identically.
 */
abstract class SvgPage extends SvgWriter {

    private static final String PAGE_OPEN = """
            <?xml version="1.0" encoding="utf-8" ?>
            <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="%s" height="%s">
            \t<clipPath id="%s">
            \t\t<rect x="-10" y="-5" width="%s" height="%s"/>
            \t</clipPath>
            \t<g clip-path="url(#%s)">
            \t\t<rect fill="white" width="%s" height="%s"/>
            """;
    private static final String PAGE_CLOSE = """
            \t</g>
            </svg>
            """;

    protected final RenderOptions options;

    SvgPage(Writer writer, RenderOptions options) {
        super(writer);
        this.options = options == null ? RenderOptions.DEFAULT : options;
    }

    /** Opens the document; the clip and backdrop extend past the canvas by 10/5px. */
    final void open(int pageNumber, int canvasWidth, int canvasHeight) {
        String clipId = "cl_" + Integer.toHexString(pageNumber + 2);
        write(PAGE_OPEN.formatted(canvasWidth, canvasHeight, clipId,
                canvasWidth + 10, canvasHeight + 5, clipId, canvasWidth + 10, canvasHeight + 5));
    }

    final void close() {
        write(PAGE_CLOSE);
        flush();
    }
}
