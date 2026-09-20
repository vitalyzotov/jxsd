package org.jxsd.parsing;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.junit.jupiter.api.Test;

/** Attribute enumeration corner cases: refs, wildcards, inline types and groups. */
class AttributeEnumeratorTest {

    private static final String EDGE = "src/test/resources/reference/library.edge.v1.xsd";

    @Test
    void attributeCornerCasesAreEnumerated() {
        Schema schema = load(EDGE);
        XmlSchemaComplexType type = complexType(schema, "AttrCornersType");

        List<SchemaAttribute> attributes = AttributeEnumerator.attributesOf(schema, type);
        Set<String> names = attributes.stream().map(SchemaAttribute::name).collect(Collectors.toSet());
        assertTrue(names.contains("globalCode"), "attribute ref");
        assertTrue(names.contains("inline"), "inline simpleType restriction");
        assertTrue(names.contains("listed"), "inline simpleType list");
        assertTrue(names.contains("outer"), "attributeGroup");
        assertTrue(names.contains("inner"), "nested attributeGroup");
        assertTrue(names.contains("*"), "anyAttribute wildcard");
    }

    private static Schema load(String path) {
        Schema schema = new Schema();
        List<String> errors = new ArrayList<>();
        schema.load(path, errors::add);
        assertTrue(errors.isEmpty(), "load errors: " + errors);
        return schema;
    }

    private static XmlSchemaComplexType complexType(Schema schema, String name) {
        for (SchemaComponent object : schema.getElements()) {
            if (name.equals(object.name()) && object.tag() instanceof XmlSchemaComplexType type) {
                return type;
            }
        }
        throw new IllegalStateException("complexType " + name + " not found");
    }
}
