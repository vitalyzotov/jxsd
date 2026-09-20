package org.jxsd.rendering;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jxsd.parsing.Schema;
import org.jxsd.parsing.SchemaComponent;
import org.jxsd.model.Diagram;
import org.jxsd.model.DiagramNode;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.junit.jupiter.api.Test;

/**
 * Locks our output for the XMLSpy conformance fixture under
 * {@code tools/xmlspy/}. The fixture and its XMLSpy counterpart are the source
 * of the recovered conventions; these snapshots guard the renderer-side result.
 * Regenerate with {@code -Dgolden.update=true}.
 */
class ConformanceRenderTest {

    private static final Path FIXTURES = Path.of("tools/xmlspy");
    private static final Path GOLDEN = Path.of("src/test/resources/golden/conformance");
    private static final boolean UPDATE = Boolean.getBoolean("golden.update");

    private static final String CNF = "http://example.org/xmlspy/conformance/v1/";
    private static final String SHR = "http://example.org/xmlspy/shared/v1/";
    private static final String EXP = "http://example.org/xmlspy/expand/v1/";

    private record Scenario(String schema, String component, String namespace, String golden) {
    }

    private record ExpansionScenario(String component, int expand, String golden) {
    }

    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario("xmlspy.conformance.v1.xsd", "SimpleText", CNF, "simple_text.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "Book", CNF, "context_book.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "Measure", CNF, "context_simple_content.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "MeasureType", CNF, "type_simple_content.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "BookType", CNF, "composite_book.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "Choice", CNF, "context_choice.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "ChoiceType", CNF, "composite_choice.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "AllType", CNF, "composite_all.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "All", CNF, "context_all.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "Occurrence", CNF, "context_occurrence.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "WildcardType", CNF, "composite_wildcard.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "Wildcard", CNF, "context_wildcard.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "Inline", CNF, "context_inline.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "GroupRefType", CNF, "composite_group_ref.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "GroupRef", CNF, "context_group_ref.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "OccurrenceType", CNF, "occurrence_type.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "Derived", CNF, "context_derived.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "Derived2", CNF, "context_derived2.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "DerivedType", CNF, "extension_derived.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "Derived2Type", CNF, "extension_derived2.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "RestrictedType", CNF, "composite_restricted.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "NestedSeqType", CNF, "composite_nested_seq.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "NestedChoiceType", CNF, "composite_nested_choice.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "ElementRefType", CNF, "composite_element_ref.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "AddressHolderType", CNF, "composite_address_holder.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "LongNamed", CNF, "context_truncated.svg"),
            new Scenario("xmlspy.conformance.v1.xsd",
                    "ExtraordinarilyLongTypeNameForTruncationTestingType", CNF, "composite_truncated.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "Empty", CNF, "simple_empty.svg"),
            new Scenario("xmlspy.conformance.v1.xsd", "EmptyType", CNF, "empty_type.svg"),
            new Scenario("xmlspy.shared.v1.xsd", "AddressType", SHR, "shared_address.svg"),
            new Scenario("xmlspy.shared.v1.xsd", "SharedNote", SHR, "shared_note.svg"));

    /** Expansion scenarios over the dedicated expand fixture ({@code -e 2/3}). */
    private static final List<ExpansionScenario> EXPANSION_SCENARIOS = List.of(
            new ExpansionScenario("ExpandCompositeType", 2, "expand_composite_type_e2.svg"),
            new ExpansionScenario("ExpandCompositeType", 3, "expand_composite_type_e3.svg"),
            new ExpansionScenario("ExpandChainType", 2, "expand_chain_type_e2.svg"),
            new ExpansionScenario("ExpandExtensionType", 2, "expand_extension_type_e2.svg"),
            new ExpansionScenario("ExpandNestedType", 2, "expand_nested_type_e2.svg"),
            new ExpansionScenario("ExpandComposite", 2, "expand_composite_e2.svg"),
            new ExpansionScenario("ExpandChain", 2, "expand_chain_e2.svg"),
            new ExpansionScenario("ExpandExtension", 2, "expand_extension_e2.svg"),
            new ExpansionScenario("ExpandNested", 2, "expand_nested_e2.svg"),
            new ExpansionScenario("ExpandElementRoot", 2, "expand_element_root_e2.svg"));

    @Test
    void conformancePagesMatchGoldenSnapshots() throws IOException {
        for (Scenario scenario : SCENARIOS) {
            byte[] actual = render(scenario);
            writeOrCompare(GOLDEN.resolve(scenario.golden()), actual, scenario.golden());
        }
    }

    @Test
    void expandedPagesMatchGoldenSnapshots() throws IOException {
        for (ExpansionScenario scenario : EXPANSION_SCENARIOS) {
            byte[] actual = render(scenario);
            writeOrCompare(GOLDEN.resolve(scenario.golden()), actual, scenario.golden());
        }
    }

    private static void writeOrCompare(Path golden, byte[] actual, String name) throws IOException {
        if (UPDATE) {
            Files.createDirectories(golden.getParent());
            Files.write(golden, actual);
            return;
        }
        assertTrue(Files.exists(golden), "missing golden " + golden);
        assertArrayEquals(Files.readAllBytes(golden), actual, name);
    }

    private static byte[] render(Scenario scenario) {
        Schema schema = new Schema();
        schema.load(FIXTURES.resolve(scenario.schema()).toString());
        XmlSchemaObject object = find(schema, scenario.component());
        assertNotNull(object, "component " + scenario.component() + " not found");

        Diagram diagram = new Diagram();
        diagram.setShowDocumentation(true);
        diagram.setSchema(schema);
        diagram.addRoot(object, scenario.namespace());
        diagram.expand();
        List<DiagramNode> roots = diagram.getRootElements();
        String svg = new PageRenderer(diagram.context(), RenderOptions.DEFAULT).render(0, roots.get(0)).orElse(null);
        assertNotNull(svg, "no page for " + scenario.component());
        return svg.getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] render(ExpansionScenario scenario) {
        Schema schema = new Schema();
        schema.load(FIXTURES.resolve("xmlspy.expand.v1.xsd").toString());
        XmlSchemaObject object = find(schema, scenario.component());
        assertNotNull(object, "component " + scenario.component() + " not found");

        Diagram diagram = new Diagram();
        diagram.setShowDocumentation(true);
        diagram.setSchema(schema);
        diagram.addRoot(object, EXP);
        diagram.expand(scenario.expand());
        List<DiagramNode> roots = diagram.getRootElements();
        String svg = new PageRenderer(diagram.context(), RenderOptions.DEFAULT).render(0, roots.get(0)).orElse(null);
        assertNotNull(svg, "no page for " + scenario.component());
        return svg.getBytes(StandardCharsets.UTF_8);
    }

    private static XmlSchemaObject find(Schema schema, String name) {
        for (SchemaComponent object : schema.getElements()) {
            if (name.equals(object.name())) {
                return object.tag();
            }
        }
        return null;
    }
}
