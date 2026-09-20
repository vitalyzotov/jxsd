package org.jxsd.model;

/**
 * An XSD wildcard particle ({@code xs:any}): a leaf with a namespace constraint,
 * not a model group. The label is derived from the constraint.
 */
public final class WildcardItem extends DiagramNode {

    private static final String LABEL_PREFIX = "any  ";

    private final String namespaceConstraint;

    WildcardItem(String namespaceConstraint, String namespace) {
        super(LABEL_PREFIX + (namespaceConstraint == null ? "##any" : namespaceConstraint), namespace);
        this.namespaceConstraint = namespaceConstraint == null ? "##any" : namespaceConstraint;
    }

    /** The wildcard namespace constraint (e.g. {@code ##any} or a namespace URI). */
    String namespaceConstraint() { return namespaceConstraint; }

    @Override
    void applyContent(NodeExpander expander) {
        // A wildcard is a leaf; it never enters the expansion frontier.
    }
}
