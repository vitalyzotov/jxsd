package org.jxsd.rendering;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;

/**
 * Rasterizes the SVG produced by {@link PageRenderer} into PNG with Apache
 * Batik. The raster is generated in pure Java; at scale {@code 1} its pixel
 * size equals the SVG's intrinsic {@code width}/{@code height}, and a scale
 * {@code s} multiplies both dimensions by {@code s}.
 *
 * <p>Callers should render the source page with {@link RenderOptions#textLength()}
 * so every text run is pinned to its measured width; a missing reference font in
 * the rasterizing environment then cannot change the layout.
 */
public final class SvgRasterizer {

    private static final Pattern SIZE = Pattern.compile("width=\"([0-9]+)\" height=\"([0-9]+)\"");

    static {
        if (System.getProperty("java.awt.headless") == null) {
            System.setProperty("java.awt.headless", "true");
        }
    }

    private SvgRasterizer() {
    }

    /** Renders {@code svg} to PNG bytes at its intrinsic size. */
    public static byte[] toPng(String svg) throws IOException {
        return toPng(svg, 1.0);
    }

    /** Renders {@code svg} to PNG bytes scaled by {@code scale} (must be &gt; 0). */
    public static byte[] toPng(String svg, double scale) throws IOException {
        if (!Double.isFinite(scale) || scale <= 0) {
            throw new IllegalArgumentException("scale must be a positive number");
        }
        PNGTranscoder transcoder = new PNGTranscoder();
        if (scale != 1.0) {
            float[] size = intrinsicSize(svg);
            transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, size[0] * (float) scale);
            transcoder.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, size[1] * (float) scale);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (StringReader reader = new StringReader(svg)) {
            transcoder.transcode(new TranscoderInput(reader), new TranscoderOutput(out));
        } catch (TranscoderException ex) {
            throw new IOException("The SVG could not be rasterized: " + ex.getMessage(), ex);
        }
        return out.toByteArray();
    }

    private static float[] intrinsicSize(String svg) throws IOException {
        Matcher matcher = SIZE.matcher(svg);
        if (!matcher.find()) {
            throw new IOException("The SVG has no width/height.");
        }
        return new float[] {Float.parseFloat(matcher.group(1)), Float.parseFloat(matcher.group(2))};
    }
}
