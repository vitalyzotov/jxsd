package org.jxsd.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.junit.jupiter.api.Test;

/** Defaults and accessors of the typed diagram node hierarchy. */
class DiagramNodeTest {

    @Test
    void elementItemDefaultsAndAccessors() {
        ElementItem item = new ElementItem("node", "ns");
        assertEquals("node", item.name());
        assertEquals("ns", item.namespace());
        assertEquals("", item.typeName());
        assertEquals("", item.typeNamespace());
        assertNull(item.type());
        assertEquals(ElementUse.LOCAL, item.use());
        assertNull(item.contentType());
        assertEquals(Occurrence.SINGLE, item.occurrence());
        assertTrue(item.attributes().isEmpty());
        assertTrue(item.children().isEmpty());
        assertNull(item.substitutionBase());
        assertFalse(item.isReference());

        item.setType(new TypeRef.Named(new QName("other", "NodeType")));
        item.setUse(ElementUse.REFERENCE);
        item.setContentType(ContentType.MIXED);

        assertEquals("NodeType", item.typeName());
        assertEquals("other", item.typeNamespace());
        assertEquals(ContentType.MIXED, item.contentType());
        assertTrue(item.isReference());
        assertTrue(ContentType.showsText(item.contentType()));
        assertTrue(ContentType.canExpand(item.contentType()));
    }

    @Test
    void groupAndTypeItemsKeepTheirOwnState() {
        ModelGroupItem group = new ModelGroupItem("", "ns");
        assertEquals(Compositor.SEQUENCE, group.compositor());
        group.setCompositor(Compositor.CHOICE);
        assertEquals(Compositor.CHOICE, group.compositor());
        // A model group is element-only by construction, so it can always expand.
        assertEquals(ContentType.ELEMENT_ONLY, group.contentType());
        assertTrue(ContentType.canExpand(group.contentType()));
        assertFalse(ContentType.showsText(group.contentType()));

        GroupRefItem reference = new GroupRefItem("NameGroup", "ns");
        assertEquals("NameGroup", reference.name());
        assertEquals(ContentType.ELEMENT_ONLY, reference.contentType());

        WildcardItem wildcard = new WildcardItem("##any", "ns");
        assertEquals("any  ##any", wildcard.name());
        assertEquals("##any", wildcard.namespaceConstraint());
        assertFalse(ContentType.canExpand(wildcard.contentType()));
        assertFalse(ContentType.showsText(wildcard.contentType()));

        TypeItem type = new TypeItem("NodeType", "ns");
        assertFalse(type.isExtensionBase());
        type.setExtensionBase(true);
        assertTrue(type.isExtensionBase());

        ElementItem child = new ElementItem("child", "ns");
        group.attach(child);
        assertEquals(1, group.children().size());
        assertSame(child, group.children().get(0));
    }

    @Test
    void expansionStateTracksTheRevealedContent() {
        ElementItem element = new ElementItem("node", "ns");
        assertFalse(element.awaitsExpansion(), "a node without content cannot expand");

        element.setContentType(ContentType.ELEMENT_ONLY);
        assertTrue(element.awaitsExpansion(), "content-bearing node starts on the frontier");
        assertFalse(element.isExpanded());

        element.markExpanded();
        assertTrue(element.isExpanded());
        assertFalse(element.awaitsExpansion(), "a revealed node leaves the frontier");
    }

    @Test
    void attachingAChildRevealsItsParent() {
        ElementItem parent = new ElementItem("node", "ns");
        parent.setContentType(ContentType.ELEMENT_ONLY);
        assertTrue(parent.awaitsExpansion());

        parent.attach(new ElementItem("child", "ns"));
        assertTrue(parent.isExpanded(), "gaining a child means the content was revealed");
        assertFalse(parent.awaitsExpansion());
    }

    @Test
    void expandingAnEmptyCompositorTerminates() {
        Diagram diagram = new Diagram();
        ModelGroupItem root = new ModelGroupItem("", "ns");
        root.setSource(new XmlSchemaSequence());
        diagram.getRootElements().add(root);

        assertEquals(1, diagram.expand(100), "an empty compositor reveals one level and stops");
        assertEquals(0, diagram.expand(1), "an exhausted tree stops growing");
    }

    @Test
    void expandIgnoresNodesWithoutExpandableContent() {
        Diagram diagram = new Diagram();
        TypeItem root = new TypeItem("EmptyType", "ns");
        root.setContentType(ContentType.SIMPLE);
        diagram.getRootElements().add(root);

        assertEquals(0, diagram.expand(1), "a type without a particle reveals no level");
    }
}
