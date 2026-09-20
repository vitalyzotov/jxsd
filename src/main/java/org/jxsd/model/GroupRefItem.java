package org.jxsd.model;

/**
 * An XSD group reference particle ({@code xs:group ref}): a named reference to a
 * model group definition, resolved and expanded into the referenced content.
 */
public final class GroupRefItem extends DiagramNode {

    GroupRefItem(String name, String namespace) {
        super(name, namespace);
        setContentType(ContentType.ELEMENT_ONLY);
    }

    @Override
    void applyContent(NodeExpander expander) {
        expander.expandParticleContent(this);
    }
}
