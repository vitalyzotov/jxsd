package org.jxsd.rendering;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaContentModel;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.junit.jupiter.api.Test;

/**
 * Drives {@link PageRenderer} from the synthetic library schema and
 * compares the produced SVG byte-for-byte with the committed snapshots. The
 * fixtures are abstract XSDs designed so every supported page shape (simple
 * element, chain, composite, context, nested) is reachable; regenerate the
 * snapshots with {@code -Dgolden.update=true}.
 */
class GoldenRenderTest {

    private static final Path FIXTURES = Path.of("src/test/resources/reference");
    private static final Path GOLDEN = Path.of("src/test/resources/golden");
    private static final boolean UPDATE = Boolean.getBoolean("golden.update");

    private static final String LIB = "http://example.org/library/v1/";
    private static final String LCL = "http://example.org/localized/v1/";
    private static final String EDGE = "http://example.org/edge/v1/";

    private record Scenario(String schema, String component, String namespace, int expand,
                            boolean docs, String language, String golden) {
    }

    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario("library.core.v1.xsd", "Comment", LIB, 1, false, "", "simple_comment.svg"),
            new Scenario("library.core.v1.xsd", "Note", LIB, 1, false, "", "simple_note.svg"),
            new Scenario("library.core.v1.xsd", "Note", LIB, 1, true, "", "simple_note_doc.svg"),
            new Scenario("library.core.v1.xsd", "tag", LIB, 1, true, "", "simple_tag_unbounded.svg"),
            new Scenario("library.core.v1.xsd", "summary", LIB, 1, true, "", "simple_summary_optional.svg"),
            new Scenario("library.core.v1.xsd", "edition", LIB, 1, false, "", "simple_edition_finite.svg"),
            new Scenario("library.core.v1.xsd", "MeasureType", LIB, 1, true, "", "type_measure.svg"),
            new Scenario("library.core.v1.xsd", "TitleType", LIB, 1, false, "", "chain_title.svg"),
            new Scenario("library.core.v1.xsd", "TitleType", LIB, 1, true, "", "chain_title_doc.svg"),
            new Scenario("library.core.v1.xsd", "SeriesType", LIB, 1, true, "", "chain_series.svg"),
            new Scenario("library.core.v1.xsd", "ChoiceSeriesType", LIB, 1, true, "", "chain_choice_series.svg"),
            new Scenario("library.core.v1.xsd", "ChoiceType", LIB, 1, true, "", "chain_choice.svg"),
            new Scenario("library.core.v1.xsd", "RepeatedGroupType", LIB, 1, true, "", "chain_repeated_group.svg"),
            new Scenario("library.core.v1.xsd", "BookType", LIB, 1, false, "", "composite_book.svg"),
            new Scenario("library.core.v1.xsd", "BookType", LIB, 1, true, "", "composite_book_doc.svg"),
            new Scenario("library.core.v1.xsd", "AudiobookType", LIB, 1, true, "", "composite_audiobook.svg"),
            new Scenario("library.core.v1.xsd", "PersonType", LIB, 1, true, "", "nested_person.svg"),
            new Scenario("library.core.v1.xsd", "Book", LIB, 1, false, "", "context_book.svg"),
            new Scenario("library.core.v1.xsd", "Book", LIB, 1, true, "", "context_book_doc.svg"),
            new Scenario("library.core.v1.xsd", "Catalog", LIB, 1, true, "", "context_catalog.svg"),
            new Scenario("library.core.v1.xsd", "Index", LIB, 1, true, "", "context_index.svg"),
            new Scenario("library.core.v1.xsd", "Select", LIB, 1, true, "", "context_select.svg"),
            new Scenario("library.core.v1.xsd", "Anonymous", LIB, 1, true, "", "context_anonymous.svg"),
            new Scenario("localized.core.v1.xsd", "Notice", LCL, 1, true, "en", "localized_notice_en.svg"),
            new Scenario("localized.core.v1.xsd", "Notice", LCL, 1, true, "ru", "localized_notice_ru.svg"),
            new Scenario("localized.core.v1.xsd", "Notice", LCL, 1, true, "de", "localized_notice_fallback.svg"),
            new Scenario("library.edge.v1.xsd", "FiniteGroupType", EDGE, 1, true, "", "edge_finite_group.svg"),
            new Scenario("library.edge.v1.xsd", "AllOptionalType", EDGE, 1, true, "", "edge_all_optional.svg"),
            new Scenario("library.edge.v1.xsd", "SingleRepeatedType", EDGE, 1, true, "", "edge_single_repeated.svg"),
            new Scenario("library.edge.v1.xsd", "FixedOccurrenceType", EDGE, 1, true, "", "edge_fixed_occurrence.svg"),
            new Scenario("library.edge.v1.xsd", "InheritedType", EDGE, 1, true, "", "edge_inherited_type.svg"),
            new Scenario("library.edge.v1.xsd", "Inherited", EDGE, 1, true, "", "edge_inherited_element.svg"),
            new Scenario("library.edge.v1.xsd", "RestrictedType", EDGE, 1, true, "", "edge_restricted.svg"),
            new Scenario("library.edge.v1.xsd", "RestrictionContentType", EDGE, 1, true, "",
                    "edge_restricted_content.svg"),
            new Scenario("library.edge.v1.xsd", "ElementRefType", EDGE, 1, true, "", "edge_element_ref.svg"),
            new Scenario("library.edge.v1.xsd", "AttrCornersType", EDGE, 1, true, "", "edge_attr_corners.svg"),
            new Scenario("library.edge.v1.xsd", "Escaped", EDGE, 1, true, "", "edge_escaped.svg"),
            new Scenario("library.edge.v1.xsd", "Blank", EDGE, 1, true, "", "edge_blank.svg"),
            new Scenario("library.edge.v1.xsd", "RepeatedHolderType", EDGE, 2, true, "",
                    "edge_repeated_holder_e2.svg"));

    @Test
    void syntheticPagesMatchGoldenSnapshots() throws IOException {
        assertTrue(SCENARIOS.size() >= 20, "expected broad page-shape coverage");
        for (Scenario scenario : SCENARIOS) {
            byte[] actual = render(scenario);
            Path golden = GOLDEN.resolve(scenario.golden());
            if (UPDATE) {
                Files.createDirectories(GOLDEN);
                Files.write(golden, actual);
                continue;
            }
            assertTrue(Files.exists(golden), "missing golden " + golden);
            assertArrayEquals(Files.readAllBytes(golden), actual, scenario.golden());
        }
    }

    @Test
    void unsupportedShapesRenderNothing() {
        // An element with an inline anonymous type renders a title-less context page.
        assertNotNull(render("library.core.v1.xsd", "Anonymous", LIB, 1));
        // A top-level group is not a page root.
        assertNull(render("library.core.v1.xsd", "NameGroup", LIB, 1));
        // A simple-content type renders as a lone type node plus its attributes tab.
        assertNotNull(render("library.core.v1.xsd", "MeasureType", LIB, 1));
        // A type mixing a nested group with attributes has no page shape.
        assertNull(render("library.edge.v1.xsd", "NestedWithAttrsType", EDGE, 1));
        // An element with simple content only is a valid simple page, not a null.
        assertNotNull(render("library.core.v1.xsd", "Comment", LIB, 1));
    }

    private static byte[] render(Scenario scenario) {
        String svg = render(scenario.schema(), scenario.component(), scenario.namespace(), scenario.expand(),
                scenario.docs(), scenario.language());
        assertNotNull(svg, "no reference page for " + scenario.component());
        return svg.getBytes(StandardCharsets.UTF_8);
    }

    private static String render(String schemaFile, String component, String namespace, int expand) {
        return render(schemaFile, component, namespace, expand, true, "");
    }

    private static String render(String schemaFile, String component, String namespace, int expand,
                                 boolean docs, String language) {
        Schema schema = new Schema();
        schema.load(FIXTURES.resolve(schemaFile).toString());
        XmlSchemaObject object = find(schema, component);
        assertNotNull(object, "component " + component + " not found");

        Diagram diagram = new Diagram();
        diagram.setShowDocumentation(docs);
        diagram.setSchema(schema);
        diagram.setLanguage(language);
        diagram.addRoot(object, namespace);
        diagram.expand(expand);
        List<DiagramNode> roots = diagram.getRootElements();
        assertEquals(1, roots.size(), "single root for " + component);
        return new PageRenderer(diagram.context(), RenderOptions.DEFAULT).render(0, roots.get(0)).orElse(null);
    }

    private static XmlSchemaObject find(Schema schema, String name) {
        for (SchemaComponent object : schema.getElements()) {
            if (name.equals(object.name())) {
                return object.tag();
            }
        }
        for (SchemaComponent object : schema.getElements()) {
            XmlSchemaObject nested = findIn(object.tag(), name);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static XmlSchemaObject findIn(Object object, String name) {
        if (object instanceof XmlSchemaElement element) {
            if (name.equals(element.getName())) {
                return element;
            }
            return findIn(element.getSchemaType(), name);
        }
        if (object instanceof XmlSchemaComplexType complexType) {
            return findInParticle(complexType.getParticle(), name);
        }
        if (object instanceof XmlSchemaGroup group) {
            return findInParticle(group.getParticle(), name);
        }
        return findInParticle(object, name);
    }

    private static XmlSchemaObject findInParticle(Object particle, String name) {
        List<?> members = items(particle);
        if (members != null) {
            for (Object member : members) {
                XmlSchemaObject found = findIn(member, name);
                if (found != null) {
                    return found;
                }
            }
        }
        if (particle instanceof XmlSchemaComplexType complexType) {
            XmlSchemaContentModel model = complexType.getContentModel();
            XmlSchemaContent content = model == null ? null : model.getContent();
            if (content instanceof XmlSchemaComplexContentExtension extension) {
                return findInParticle(extension.getParticle(), name);
            }
        }
        return null;
    }

    private static List<?> items(Object particle) {
        if (particle instanceof XmlSchemaSequence sequence) {
            return sequence.getItems();
        }
        if (particle instanceof XmlSchemaChoice choice) {
            return choice.getItems();
        }
        if (particle instanceof XmlSchemaAll all) {
            return all.getItems();
        }
        return null;
    }
}
