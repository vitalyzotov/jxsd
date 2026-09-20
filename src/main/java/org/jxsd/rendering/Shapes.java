package org.jxsd.rendering;

/** Path geometry for the reference node shapes (element, complex type, group). */
public final class Shapes {

    public static final int BEVEL = 5;

    /** Outline attributes of a solid (required) node border. */
    public static final String SOLID_STROKE = "fill=\"none\" stroke=\"black\" stroke-width=\"1\" "
            + "stroke-linecap=\"square\" stroke-miterlimit=\"4\" transform=\"translate(0.5 0.5)\"";

    public enum ShapeKind {
        ELEMENT,
        TYPE,
        GROUP
    }

    private Shapes() {
    }

    /** Fill path of a node shape with the given bounds. */
    public static String path(ShapeKind kind, int x, int y, int width, int height) {
        return switch (kind) {
            case ELEMENT -> rectangle(x, y, width, height);
            case TYPE -> leftBeveledHexagon(x, y, width, height);
            case GROUP -> octagon(x, y, width, height);
        };
    }

    /** Outline path: the reference insets the stroke by one pixel on right/bottom. */
    public static String outline(ShapeKind kind, int x, int y, int width, int height) {
        return path(kind, x, y, width - 1, height - 1);
    }

    private static String rectangle(int x, int y, int width, int height) {
        return "M" + x + " " + y
                + "L" + (x + width) + " " + y
                + "L" + (x + width) + " " + (y + height)
                + "L" + x + " " + (y + height) + "Z";
    }

    private static String leftBeveledHexagon(int x, int y, int width, int height) {
        return "M" + x + " " + (y + BEVEL)
                + "L" + (x + BEVEL) + " " + y
                + "L" + (x + width) + " " + y
                + "L" + (x + width) + " " + (y + height)
                + "L" + (x + BEVEL) + " " + (y + height)
                + "L" + x + " " + (y + height - BEVEL)
                + "L" + x + " " + (y + BEVEL) + "Z";
    }

    private static String octagon(int x, int y, int width, int height) {
        return "M" + x + " " + (y + BEVEL)
                + "L" + (x + BEVEL) + " " + y
                + "L" + (x + width - BEVEL) + " " + y
                + "L" + (x + width) + " " + (y + BEVEL)
                + "L" + (x + width) + " " + (y + height - BEVEL)
                + "L" + (x + width - BEVEL) + " " + (y + height)
                + "L" + (x + BEVEL) + " " + (y + height)
                + "L" + x + " " + (y + height - BEVEL)
                + "L" + x + " " + (y + BEVEL) + "Z";
    }
}
