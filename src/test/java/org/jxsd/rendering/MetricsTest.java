package org.jxsd.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Locks the geometry recovered from the XMLSpy conformance diagrams. Changing a
 * value here is a deliberate reference change and must be accompanied by a fresh
 * corpus under {@code tools/xmlspy/}.
 */
class MetricsTest {

    @Test
    void siblingGapsMatchTheRecoveredPitch() {
        assertEquals(12, Metrics.SIBLING_ELEMENT_GAP);
        assertEquals(7, Metrics.SIBLING_ATTRIBUTE_GAP);
        assertTrue(Metrics.SIBLING_ELEMENT_GAP > Metrics.SIBLING_ATTRIBUTE_GAP,
                "element stacks breathe more than attribute stacks in the reference");
    }

    @Test
    void truncationWidthMatchesTheWidestRetainedLabel() {
        assertEquals(170, Metrics.LABEL_TRUNCATION_WIDTH);
    }

    @Test
    void documentationWrapWidthMatchesTheReferenceColumn() {
        assertEquals(115, Metrics.DOCUMENTATION_WRAP_WIDTH);
    }

    @Test
    void longLabelsAreTruncatedWithAnEllipsis() {
        assertEquals("Book", Labels.truncate("Book"));
        String truncated = Labels.truncate("ExtraordinarilyLongChildElementNameForTruncation");
        assertTrue(truncated.endsWith("..."), truncated);
        assertTrue(truncated.length() < "ExtraordinarilyLongChildElementNameForTruncation".length());
    }
}
