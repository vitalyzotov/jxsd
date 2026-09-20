package org.jxsd.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.jxsd.parsing.SchemaAttribute;
import org.junit.jupiter.api.Test;

/** Attribute and metrics primitives used by the renderer. */
class RenderingSupportTest {

    @Test
    void xsdAttributeAccessors() {
        SchemaAttribute attribute = new SchemaAttribute("code", null);
        assertEquals("code", attribute.name());
        assertNull(attribute.tag());
    }

    @Test
    void metricsHandleEmptyAndUnknownText() {
        SegoeUiMetrics metrics = SegoeUiMetrics.instance();
        assertEquals(0f, metrics.advanceWidth("", 12f, SegoeUiMetrics.FAMILY, "400", "normal"));

        float unknown = metrics.advanceWidth("\u2603", 12f, SegoeUiMetrics.FAMILY, "400", "normal");
        assertEquals(1024 * 12f / SegoeUiMetrics.UNITS_PER_EM, unknown, 0.0001f);
    }

    @Test
    void metricsMatchTheGeneratedTable() {
        SegoeUiMetrics metrics = SegoeUiMetrics.instance();
        assertEquals(7.7402344f, metrics.advanceWidth("A", 12f, SegoeUiMetrics.FAMILY, "400", "normal"), 0.0001f);
        assertEquals(52.617188f, metrics.advanceWidth("ETEBType", 12f, SegoeUiMetrics.FAMILY, "600", "normal"), 0.01f);
        assertEquals(111.890625f,
                metrics.advanceWidth("ResourceDescription", 12f, SegoeUiMetrics.FAMILY, "600", "normal"), 0.01f);
        assertEquals(122.003906f,
                metrics.advanceWidth("Русское уведомление.", 12f, SegoeUiMetrics.FAMILY, "400", "normal"), 0.01f);
    }

    @Test
    void coordinateFormattingTrimsAndRounds() {
        assertEquals("0", SegoeUiMetrics.formatCoordinate(0));
        assertEquals("1", SegoeUiMetrics.formatCoordinate(1.0));
        assertEquals("0.5", SegoeUiMetrics.formatCoordinate(0.5));
        assertEquals("-1.25", SegoeUiMetrics.formatCoordinate(-1.25));
    }
}
