package org.jxsd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.jxsd.parsing.ComponentKind;
import org.jxsd.parsing.Schema;
import org.jxsd.parsing.SchemaComponent;
import org.jxsd.model.Compositor;
import org.jxsd.model.Diagram;
import org.jxsd.model.DiagramNode;
import org.jxsd.model.ElementItem;
import org.jxsd.model.GroupRefItem;
import org.jxsd.model.ModelGroupItem;
import org.jxsd.model.TypeItem;
import org.jxsd.model.WildcardItem;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.junit.jupiter.api.Test;

/**
 * The synthetic library fixtures must load offline, resolve their include and
 * import, and expand the compositors the reference renderer relies on.
 */
class SchemaLoadTest {

    private static final String LIBRARY = "src/test/resources/reference/library.core.v1.xsd";
    private static final String EDGE_LIBRARY = "src/test/resources/reference/library.edge.v1.xsd";
    private static final String LIB = "http://example.org/library/v1/";
    private static final String EDGE = "http://example.org/edge/v1/";
    private static final String SHARED = "http://example.org/shared/v1/";

    @Test
    void librarySchemaLoadsWithoutErrors() {
        Schema schema = new Schema();
        List<String> errors = new ArrayList<>();
        schema.load(LIBRARY, errors::add);

        assertTrue(errors.isEmpty(), "load errors: " + errors);

        List<SchemaComponent> components = schema.getElements();
        assertTrue(contains(components, ComponentKind.ELEMENT, "Book"), "Book element");
        assertTrue(contains(components, ComponentKind.COMPLEX_TYPE, "BookType"), "BookType");
        assertTrue(contains(components, ComponentKind.COMPLEX_TYPE, "AudiobookType"), "AudiobookType");
        assertTrue(contains(components, ComponentKind.SIMPLE_TYPE, "CodeType"), "included CodeType");
        assertTrue(contains(components, ComponentKind.SIMPLE_TYPE, "RatingType"), "included RatingType");
        assertTrue(contains(components, ComponentKind.GROUP, "NameGroup"), "NameGroup");
        assertTrue(contains(components, ComponentKind.ATTRIBUTE_GROUP, "AuditAttrs"), "AuditAttrs");
        assertNotNull(schema.findType(new QName(SHARED, "AddressType")), "imported AddressType");
    }

    @Test
    void groupReferenceExpandsIntoAnEnvelope() {
        DiagramNode root = expand("GroupRefType");

        DiagramNode outer = onlyChild(root);
        assertEquals(Compositor.SEQUENCE, group(outer).compositor());
        assertEquals(2, outer.children().size());
        DiagramNode envelope = outer.children().get(0);
        assertInstanceOf(GroupRefItem.class, envelope);
        assertEquals("NameGroup", envelope.name());
        assertEquals(3, envelope.children().size());
    }

    @Test
    void wildcardExpandsToAnAnyNode() {
        DiagramNode root = expand("WildcardType");

        DiagramNode outer = onlyChild(root);
        assertEquals(2, outer.children().size());
        DiagramNode wildcard = outer.children().get(1);
        assertInstanceOf(WildcardItem.class, wildcard);
        assertTrue(wildcard.name().startsWith("any"), wildcard.name());
    }

    @Test
    void allCompositorExpands() {
        DiagramNode root = expand("AllType");

        DiagramNode all = onlyChild(root);
        assertEquals(Compositor.ALL, group(all).compositor());
        assertEquals(2, all.children().size());
    }

    @Test
    void extensionWithoutParticleInheritsBaseContent() {
        DiagramNode root = expand(EDGE_LIBRARY, EDGE, "InheritedType");

        DiagramNode base = onlyChild(root);
        assertInstanceOf(TypeItem.class, base);
        assertTrue(((TypeItem) base).isExtensionBase(), "base type node");
        assertEquals("InheritBaseType", base.name());

        DiagramNode outer = onlyChild(base);
        assertEquals(Compositor.SEQUENCE, group(outer).compositor());
        assertEquals(1, outer.children().size());
        assertEquals("baseField", outer.children().get(0).name());
    }

    @Test
    void extensionWithParticleKeepsBaseAndDerivedContent() {
        DiagramNode root = expand("AudiobookType");

        DiagramNode base = root.children().get(0);
        assertInstanceOf(TypeItem.class, base);
        assertTrue(((TypeItem) base).isExtensionBase(), "base type node");
        assertEquals("BookType", base.name());

        DiagramNode derived = root.children().get(1);
        assertInstanceOf(ModelGroupItem.class, derived);
        assertEquals(2, derived.children().size());
    }

    @Test
    void elementReferenceResolvesItsTarget() {
        DiagramNode root = expand(EDGE_LIBRARY, EDGE, "ElementRefType");

        DiagramNode outer = onlyChild(root);
        assertEquals(2, outer.children().size());
        DiagramNode reference = outer.children().get(0);
        assertInstanceOf(ElementItem.class, reference);
        assertTrue(((ElementItem) reference).isReference(), "element reference flag");
        assertEquals("RefTarget", reference.name());
    }

    @Test
    void groupWithAllParticleExpands() {
        DiagramNode root = expand(EDGE_LIBRARY, EDGE, "UsesAllGroupType");

        DiagramNode envelope = onlyChild(onlyChild(root));
        assertInstanceOf(GroupRefItem.class, envelope);
        assertEquals(2, envelope.children().size());
    }

    @Test
    void unresolvedGroupReferenceFallsBackToAny() {
        DiagramNode root = expand(EDGE_LIBRARY, EDGE, "MissingGroupType");

        DiagramNode envelope = onlyChild(onlyChild(root));
        assertInstanceOf(GroupRefItem.class, envelope);
        assertEquals(1, envelope.children().size());
        assertInstanceOf(WildcardItem.class, envelope.children().get(0));
        assertTrue(envelope.children().get(0).name().startsWith("any"));
    }

    @Test
    void substitutionGroupAddsTheSubstitute() {
        Schema schema = new Schema();
        schema.load(EDGE_LIBRARY);
        Diagram diagram = new Diagram();
        diagram.setSchema(schema);
        diagram.addRoot(find(schema, "AbstractMessage"), EDGE);
        diagram.expand();

        assertEquals(2, diagram.getRootElements().size());
        DiagramNode base = diagram.getRootElements().get(0);
        DiagramNode substitute = diagram.getRootElements().get(1);
        assertEquals("ConcreteMessage", substitute.name());
        assertInstanceOf(ElementItem.class, substitute);
        assertSame(base, ((ElementItem) substitute).substitutionBase(),
                "substitute inherits from the abstract element");
    }

    @Test
    void expandGrowsTheTreeByTheRequestedNumberOfLevels() {
        Schema schema = new Schema();
        schema.load(LIBRARY);
        Diagram diagram = new Diagram();
        diagram.setSchema(schema);
        diagram.addRoot(find(schema, "Book"), LIB);

        assertEquals(0, diagram.expand(0), "a zero-level request grows nothing");
        assertEquals(1, diagram.expand(1), "the first level reveals the book content");
        int grown = diagram.expand(100);
        assertTrue(grown < 100, "the finite tree is exhausted before the level cap");
        assertEquals(0, diagram.expand(1), "an exhausted tree stops growing");
    }

    @Test
    void expandRevealsExactlyTheRequestedNumberOfLevels() {
        Schema schema = new Schema();
        schema.load(LIBRARY);
        Diagram diagram = new Diagram();
        diagram.setSchema(schema);
        diagram.addRoot(find(schema, "Book"), LIB);

        diagram.expand(1);
        DiagramNode content = onlyChild(diagram.getRootElements().get(0));
        DiagramNode title = content.children().get(0);
        assertEquals("title", title.name());
        assertTrue(title.children().isEmpty(), "level 2 stays hidden at -e 1");

        diagram.expand(1);
        assertFalse(title.children().isEmpty(), "the next pass reveals level 2");
    }

    @Test
    void expandingASimpleElementGrowsNothing() {
        Schema schema = new Schema();
        schema.load(LIBRARY);
        Diagram diagram = new Diagram();
        diagram.setSchema(schema);
        diagram.addRoot(find(schema, "Comment"), LIB);

        assertEquals(0, diagram.expand(3), "a simple element has no children to reveal");
    }

    @Test
    void negativeExpandLevelIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Diagram().expand(-1));
    }

    private static DiagramNode expand(String typeName) {
        return expand(LIBRARY, LIB, typeName);
    }

    private static DiagramNode expand(String schemaFile, String namespace, String typeName) {
        Schema schema = new Schema();
        schema.load(schemaFile);
        XmlSchemaObject tag = find(schema, typeName);

        Diagram diagram = new Diagram();
        diagram.setShowDocumentation(true);
        diagram.setSchema(schema);
        diagram.addRoot(tag, namespace);
        diagram.expand();

        assertEquals(1, diagram.getRootElements().size(), typeName);
        return diagram.getRootElements().get(0);
    }

    private static DiagramNode onlyChild(DiagramNode root) {
        assertEquals(1, root.children().size(), root.name());
        return root.children().get(0);
    }

    private static ModelGroupItem group(DiagramNode node) {
        return assertInstanceOf(ModelGroupItem.class, node);
    }

    private static XmlSchemaObject find(Schema schema, String name) {
        for (SchemaComponent object : schema.getElements()) {
            if (name.equals(object.name())) {
                return object.tag();
            }
        }
        throw new IllegalStateException("component " + name + " not found");
    }

    private static boolean contains(List<SchemaComponent> components, ComponentKind type, String name) {
        return components.stream().anyMatch(c -> c.kind() == type && name.equals(c.name()));
    }
}
