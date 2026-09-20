package org.jxsd.model;

/** A complex-type node; {@link #isExtensionBase()} marks the synthetic base of an extension. */
public final class TypeItem extends DiagramNode {

    private boolean extensionBase;

    TypeItem(String name, String namespace) {
        super(name, namespace);
    }

    /** True for the synthetic node that carries an {@code xs:extension} base type. */
    public boolean isExtensionBase() { return extensionBase; }
    void setExtensionBase(boolean value) { extensionBase = value; }

    @Override
    void applyContent(NodeExpander expander) {
        expander.expandType(this);
    }
}
