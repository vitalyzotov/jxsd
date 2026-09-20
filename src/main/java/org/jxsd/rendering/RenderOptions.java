package org.jxsd.rendering;

/**
 * Rendering knobs for the reference renderer.
 *
 * <p>When {@code textLength} is set every text run carries a {@code textLength}
 * attribute fixing it to the width computed from the bundled metrics, so the
 * layout survives a font substitution in the viewer.
 */
public record RenderOptions(boolean textLength) {

    public static final RenderOptions DEFAULT = new RenderOptions(false);
}
