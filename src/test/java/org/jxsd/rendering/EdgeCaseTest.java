package org.jxsd.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;

import org.jxsd.rendering.Shapes.ShapeKind;
import org.jxsd.model.Compositor;
import org.jxsd.model.Occurrence;
import org.junit.jupiter.api.Test;

/** Boundary and error-path checks for the reference rendering primitives. */
class EdgeCaseTest {

    private static final RenderOptions MODERN = RenderOptions.DEFAULT;

    @Test
    void constructorsRejectNullWriter() {
        assertThrows(IllegalArgumentException.class, () -> new ChainPage(null));
        assertThrows(IllegalArgumentException.class, () -> new ChainPage(null, MODERN));
        assertThrows(IllegalArgumentException.class, () -> new CompositePage(null, MODERN));
        assertThrows(IllegalArgumentException.class, () -> new ContextPage(null));
        assertThrows(IllegalArgumentException.class, () -> new ContextPage(null, MODERN));
        assertThrows(IllegalArgumentException.class, () -> new NestedPage(null, MODERN));
        assertThrows(IllegalArgumentException.class, () -> new SimpleElementPage(null));
        assertThrows(IllegalArgumentException.class, () -> new SimpleElementPage(null, MODERN));
        assertThrows(IllegalArgumentException.class, () -> new ElementNode(null));
        assertThrows(IllegalArgumentException.class, () -> new ElementNode(null, MODERN));
        assertThrows(IllegalArgumentException.class, () -> new TypeNode(null, MODERN));
        assertThrows(IllegalArgumentException.class, () -> new GroupNode(null));
        assertThrows(IllegalArgumentException.class, () -> new GroupNode(null, MODERN));
        assertThrows(IllegalArgumentException.class, () -> new Connector(null));
    }

    @Test
    void groupNodeRejectsUnsupportedCompositors() {
        GroupNode group = new GroupNode(new StringWriter());
        assertThrows(IllegalArgumentException.class,
                () -> group.render(0, 0, (Compositor) null));
    }

    @Test
    void renderRejectsNullRoot() {
        assertTrue(new PageRenderer().render(0, null).isEmpty());
        assertTrue(new PageRenderer(MODERN).render(0, null).isEmpty());
    }

    @Test
    void failingWriterIsWrappedAsUnchecked() {
        assertThrows(UncheckedIOException.class, () -> new Connector(failingWriter()).line(0, 0, 1, 1));
        assertThrows(UncheckedIOException.class,
                () -> new SimpleElementPage(failingWriter()).render(0, "Name"));
        assertThrows(UncheckedIOException.class,
                () -> new TypeNode(failingWriter(), MODERN).render(0, 0, "Name", null));
        assertThrows(UncheckedIOException.class,
                () -> new GroupNode(failingWriter()).render(0, 0, Compositor.SEQUENCE));
        assertThrows(UncheckedIOException.class,
                () -> new ChainPage(failingWriter()).render(0, "Name", null,
                        Compositor.SEQUENCE,
                        List.of(new ElementView("child", null, false, false, true))));
    }

    @Test
    void prohibitedElementIsCrossedOut() {
        StringWriter writer = new StringWriter();
        new ElementNode(writer).render(10, 20, "forbidden", null, false, false, false, Occurrence.of(0, 0));
        String svg = writer.toString();
        assertTrue(svg.contains("d=\"M10 20L"), svg);
        assertTrue(svg.contains("d=\"M10 40L"), svg);
    }

    @Test
    void optionalChildConnectorIsADashedHorizontalRun() {
        StringWriter writer = new StringWriter();
        new Connector(writer).dashedHorizontal(172, 43, 186);
        String svg = writer.toString();
        // Several dashes span from the branch to the child edge (childX - 1).
        assertTrue(svg.contains("M172 42.5"), svg);
        assertTrue(svg.contains("M178 42.5"), svg);
        assertTrue(svg.contains("M184 42.5"), svg);
        assertTrue(svg.contains("186.5"), svg);
    }

    @Test
    void unboundedOccurrenceUsesSplitRunsAndInfinityGlyph() {
        StringWriter writer = new StringWriter();
        new ElementNode(writer).render(0, 0, "tag", null, false, false, false, Occurrence.of(1, -1));
        String svg = writer.toString();
        assertTrue(svg.contains("font-family=\"Segoe UI\""), svg);
        assertTrue(svg.contains("\u221e"), svg);
        assertTrue(svg.contains(".."), svg);
    }

    @Test
    void elementShapeIsARectangle() {
        assertEquals("M0 0L10 0L10 20L0 20Z",
                Shapes.path(ShapeKind.ELEMENT, 0, 0, 10, 20));
        assertEquals("M2 3L11 3L11 22L2 22Z",
                Shapes.outline(ShapeKind.ELEMENT, 2, 3, 10, 20));
    }

    @Test
    void blankDocumentationCollapsesToAnEmptyLine() {
        Documentation empty = Documentation.of("");
        assertEquals(1, empty.lines().size());
        assertEquals(12, empty.height(12));
        assertEquals(12, empty.height());

        Documentation blank = Documentation.of("   ");
        assertEquals(1, blank.lines().size());
        assertEquals(1, blank.height(12));
    }

    @Test
    void documentationEscapesMarkup() {
        assertEquals("&amp;&lt;&gt;&quot;", Documentation.escape("&<>\""));
    }

    private static Writer failingWriter() {
        return new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException {
                throw new IOException("write failed");
            }

            @Override
            public void flush() throws IOException {
                throw new IOException("flush failed");
            }

            @Override
            public void close() {
                // no-op
            }
        };
    }
}
