package org.jxsd.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.jxsd.parsing.ComponentKind;
import org.jxsd.parsing.Schema;
import org.jxsd.parsing.SchemaComponent;
import org.junit.jupiter.api.Test;

/** Tests for root reference resolution (bare, prefix, Clark and kind). */
class RootSelectorTest {

    private static final String LIB = "http://example.org/library/v1/";
    private static final String AMBIGUOUS = "http://example.org/ambiguous/v1/";

    private static Schema load(String file) {
        Schema schema = new Schema();
        schema.load("src/test/resources/reference/" + file);
        return schema;
    }

    private static IllegalArgumentException selectError(Schema schema, String reference, String kind) {
        return assertThrows(IllegalArgumentException.class,
                () -> RootSelector.select(schema, reference, kind, Map.of()));
    }

    @Test
    void resolvesABareUniqueName() {
        SchemaComponent root = RootSelector.select(load("library.core.v1.xsd"), "Book", null, Map.of());
        assertEquals("Book", root.name());
        assertEquals(ComponentKind.ELEMENT, root.kind());
        assertEquals(LIB, root.namespace());
    }

    @Test
    void resolvesATypeByBareName() {
        SchemaComponent root = RootSelector.select(load("library.core.v1.xsd"), "BookType", null, Map.of());
        assertEquals("BookType", root.name());
        assertEquals(ComponentKind.COMPLEX_TYPE, root.kind());
    }

    @Test
    void kindFiltersTheCandidates() {
        Schema schema = load("library.core.v1.xsd");
        assertEquals(ComponentKind.ELEMENT, RootSelector.select(schema, "Book", "element", Map.of()).kind());
        assertTrue(selectError(schema, "Book", "complexType").getMessage().contains("not found"));
    }

    @Test
    void unknownKindIsRejected() {
        Schema schema = load("library.core.v1.xsd");
        assertTrue(selectError(schema, "Book", "widget").getMessage().contains("Unknown root kind"));
    }

    @Test
    void unknownComponentIsReported() {
        Schema schema = load("library.core.v1.xsd");
        assertTrue(selectError(schema, "Nope", null).getMessage().contains("not found"));
    }

    @Test
    void clarkNotationSelectsByNamespace() {
        Schema schema = load("library.core.v1.xsd");
        assertEquals("Book", RootSelector.select(schema, "{" + LIB + "}Book", null, Map.of()).name());
        assertTrue(selectError(schema, "{http://example.org/other/}Book", null)
                .getMessage().contains("not found"));
    }

    @Test
    void malformedClarkNotationIsReported() {
        Schema schema = load("library.core.v1.xsd");
        assertTrue(selectError(schema, "{http://example.org/library/v1/Book", null)
                .getMessage().contains("missing '}'"));
    }

    @Test
    void prefixFromTheSchemaIsResolved() {
        Schema schema = load("ambiguous.core.v1.xsd");
        SchemaComponent root = RootSelector.select(schema, "tns:Shared", "element", Map.of());
        assertEquals(ComponentKind.ELEMENT, root.kind());
        assertEquals(AMBIGUOUS, root.namespace());
    }

    @Test
    void unknownPrefixNeedsABinding() {
        Schema schema = load("library.core.v1.xsd");
        assertTrue(selectError(schema, "zz:Book", null).getMessage().contains("Unknown namespace prefix"));

        SchemaComponent root = RootSelector.select(schema, "zz:Book", null, Map.of("zz", LIB));
        assertEquals("Book", root.name());
    }

    @Test
    void ambiguousBareNameIsRejected() {
        Schema schema = load("ambiguous.core.v1.xsd");
        assertTrue(selectError(schema, "Shared", null).getMessage().contains("ambiguous"));
        assertEquals(ComponentKind.ELEMENT, RootSelector.select(schema, "Shared", "element", Map.of()).kind());
        assertEquals(ComponentKind.COMPLEX_TYPE, RootSelector.select(schema, "Shared", "complexType", Map.of()).kind());
    }
}
