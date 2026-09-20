package org.jxsd.model;

/**
 * The occurrence range of a diagram node ({@code minOccurs..maxOccurs}). An
 * unbounded maximum is represented by {@link #UNBOUNDED}, replacing the bare
 * {@code -1} sentinel that used to be threaded through the renderers.
 */
public record Occurrence(int min, int max) {

    /** Sentinel maximum for an unbounded occurrence ({@code maxOccurs="unbounded"}). */
    public static final int UNBOUNDED = -1;

    /** The common required, non-repeated occurrence ({@code 1..1}). */
    public static final Occurrence SINGLE = new Occurrence(1, 1);

    public static Occurrence of(int min, int max) {
        return min == 1 && max == 1 ? SINGLE : new Occurrence(min, max);
    }

    public static Occurrence unbounded(int min) {
        return new Occurrence(min, UNBOUNDED);
    }

    /** True when {@code maxOccurs="unbounded"}. */
    public boolean isUnbounded() {
        return max == UNBOUNDED;
    }

    /** True when the node is drawn repeated (unbounded or more than one). */
    public boolean isRepeated() {
        return max == UNBOUNDED || max > 1;
    }

    /** True when {@code maxOccurs="0"} (a prohibited node, drawn crossed out). */
    public boolean isProhibited() {
        return max == 0;
    }
}
