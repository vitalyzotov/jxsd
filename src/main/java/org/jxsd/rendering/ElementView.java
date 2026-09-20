package org.jxsd.rendering;

import org.jxsd.model.Occurrence;

/**
 * An element as laid out inside a page: its prefixed label, raw documentation,
 * expand and text-content state, plus its occurrence. Shared by the chain,
 * composite, context, extension and nested page layouts. The pitch and bounds
 * helpers centralize the per-element arithmetic those pages used to repeat, and
 * the documentation is parsed once here rather than per measurement.
 */
public final class ElementView {

    private static final int ROW_HEIGHT = 21;
    private static final int REPEATED_OFFSET = 3;
    private static final int DOCUMENTATION_INSET = 5;

    private final String name;
    private final String documentation;
    private final boolean canExpand;
    private final boolean expanded;
    private final boolean showsText;
    private final Occurrence occurrence;
    private final Documentation parsedDocumentation;

    public ElementView(String name, String documentation, boolean canExpand, boolean expanded,
                       boolean showsText, Occurrence occurrence) {
        this.name = name;
        this.documentation = documentation;
        this.canExpand = canExpand;
        this.expanded = expanded;
        this.showsText = showsText;
        this.occurrence = occurrence;
        this.parsedDocumentation = documentation == null ? null : Documentation.of(documentation);
    }

    /** A required, non-expanded child. */
    public ElementView(String name, String documentation,
                       boolean canExpand, boolean expanded, boolean showsText) {
        this(name, documentation, canExpand, expanded, showsText, Occurrence.SINGLE);
    }

    public String name() { return name; }

    public String documentation() { return documentation; }

    public boolean canExpand() { return canExpand; }

    public boolean expanded() { return expanded; }

    public boolean showsText() { return showsText; }

    public Occurrence occurrence() { return occurrence; }

    /** Vertical pitch of the row, documentation included, matching the reference. */
    int consumed() {
        if (parsedDocumentation == null) {
            return occurrence.isRepeated() ? 43 : ROW_HEIGHT;
        }
        return (occurrence.isRepeated() ? 46 : 27)
                + parsedDocumentation.height(Documentation.LINE_HEIGHT);
    }

    /** Bottom whitespace below the row when computing a canvas or container bottom. */
    int bottomMargin() {
        return occurrence.isRepeated() || documentation != null ? 10 : 14;
    }

    /** Visual right edge of the node, a repeated copy's offset included. */
    int visualRight() {
        return (occurrence.isRepeated() ? REPEATED_OFFSET : 0)
                + NodeGeometry.elementWidth(name, canExpand);
    }

    /** Right edge of the documentation column, or {@code 0} without documentation. */
    int documentationRight() {
        return parsedDocumentation == null
                ? 0 : DOCUMENTATION_INSET + parsedDocumentation.width();
    }
}
