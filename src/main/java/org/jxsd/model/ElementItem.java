package org.jxsd.model;

/** An element node with its declared type and how it was declared. */
public final class ElementItem extends DiagramNode {

    private TypeRef type;
    private ElementUse use = ElementUse.LOCAL;
    private DiagramNode substitutionBase;

    ElementItem(String name, String namespace) {
        super(name, namespace);
    }

    /** The element's {@code {type definition}}: a named reference or an inline type. */
    public TypeRef type() { return type; }
    void setType(TypeRef value) { type = value; }

    /** The declared type's local name; empty for an inline anonymous type. */
    public String typeName() {
        return type instanceof TypeRef.Named named ? named.name().getLocalPart() : "";
    }

    /** The declared type's namespace; empty for an inline anonymous type. */
    public String typeNamespace() {
        return type instanceof TypeRef.Named named ? named.name().getNamespaceURI() : "";
    }

    /** How the element was declared (global/local declaration, reference or substitute). */
    ElementUse use() { return use; }
    void setUse(ElementUse value) { use = value == null ? ElementUse.LOCAL : value; }

    public boolean isReference() { return use == ElementUse.REFERENCE; }

    /** The abstract element this node substitutes, if any. */
    public DiagramNode substitutionBase() { return substitutionBase; }
    void setSubstitutionBase(DiagramNode value) { substitutionBase = value; }

    @Override
    void applyContent(NodeExpander expander) {
        expander.expandElement(this);
    }
}
