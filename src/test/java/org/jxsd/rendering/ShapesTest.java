package org.jxsd.rendering;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jxsd.rendering.Shapes.ShapeKind;
import org.junit.jupiter.api.Test;

/** Shape geometry must match the paths emitted by the reference pages. */
class ShapesTest {

    @Test
    void typeHexagonMatchesExpectedPath() {
        assertEquals("M0 59L5 54L74 54L74 76L5 76L0 71L0 59Z",
                Shapes.path(ShapeKind.TYPE, 0, 54, 74, 22));
        assertEquals("M0 59L5 54L73 54L73 75L5 75L0 70L0 59Z",
                Shapes.outline(ShapeKind.TYPE, 0, 54, 74, 22));
    }

    @Test
    void typeHexagonMatchesOffsetOrigin() {
        assertEquals("M0 528L5 523L129 523L129 545L5 545L0 540L0 528Z",
                Shapes.path(ShapeKind.TYPE, 0, 523, 129, 22));
    }

    @Test
    void groupOctagonMatchesExpectedPath() {
        assertEquals("M104 81L109 76L134 76L139 81L139 92L134 97L109 97L104 92L104 81Z",
                Shapes.path(ShapeKind.GROUP, 104, 76, 35, 21));
        assertEquals("M104 81L109 76L133 76L138 81L138 91L133 96L109 96L104 91L104 81Z",
                Shapes.outline(ShapeKind.GROUP, 104, 76, 35, 21));
    }
}
