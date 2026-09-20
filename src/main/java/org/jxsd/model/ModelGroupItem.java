package org.jxsd.model;

/**
 * An anonymous XSD model group ({@code xs:sequence}, {@code xs:choice} or
 * {@code xs:all}). A named group reference is a separate {@link GroupRefItem}.
 */
public final class ModelGroupItem extends DiagramNode {

    private Compositor compositor = Compositor.SEQUENCE;

    ModelGroupItem(String name, String namespace) {
        super(name, namespace);
        setContentType(ContentType.ELEMENT_ONLY);
    }

    public Compositor compositor() { return compositor; }
    void setCompositor(Compositor value) {
        compositor = value == null ? Compositor.SEQUENCE : value;
    }

    @Override
    void applyContent(NodeExpander expander) {
        expander.expandParticleContent(this);
    }
}
