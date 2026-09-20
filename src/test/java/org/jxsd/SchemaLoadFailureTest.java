package org.jxsd;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jxsd.parsing.ComponentKind;
import org.jxsd.parsing.Schema;
import org.jxsd.parsing.SchemaComponent;
import org.junit.jupiter.api.Test;

/** Failure and recovery paths of the schema loader. */
class SchemaLoadFailureTest {

    private static final String REFERENCE = "src/test/resources/reference/";

    @Test
    void missingFileIsReported() {
        Schema schema = new Schema();
        List<String> errors = new ArrayList<>();
        schema.load(REFERENCE + "does-not-exist.core.v1.xsd", errors::add);

        assertFalse(errors.isEmpty());
        assertTrue(schema.getElements().isEmpty());
    }

    @Test
    void missingDependencyIsTolerated() {
        Schema schema = new Schema();
        List<String> errors = new ArrayList<>();
        schema.load(REFERENCE + "missing-dependency.core.v1.xsd", errors::add);

        assertTrue(errors.stream().anyMatch(error -> error.contains("does-not-exist")),
                "missing include reported: " + errors);
        assertTrue(contains(schema.getElements(), ComponentKind.ELEMENT, "Standalone"),
                "parsed components survive the missing dependency");
    }

    @Test
    void malformedSchemaIsReported() {
        Schema schema = new Schema();
        List<String> errors = new ArrayList<>();
        schema.load(REFERENCE + "malformed.core.v1.xsd", errors::add);

        assertFalse(errors.isEmpty());
        assertTrue(schema.getElements().isEmpty());
    }

    @Test
    void credentialsAndCollectionAreAccessible() {
        Schema schema = new Schema();
        schema.setCredentials("bob", "secret");
        assertNotNull(schema.getCollection());

        List<String> errors = new ArrayList<>();
        schema.load(REFERENCE + "library.core.v1.xsd", errors::add);
        assertTrue(errors.isEmpty(), "load errors: " + errors);
        assertNotNull(schema.getCollection());
    }

    private static boolean contains(List<SchemaComponent> components, ComponentKind type, String name) {
        return components.stream().anyMatch(c -> c.kind() == type && name.equals(c.name()));
    }
}
