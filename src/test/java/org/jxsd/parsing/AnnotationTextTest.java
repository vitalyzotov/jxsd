package org.jxsd.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.junit.jupiter.api.Test;

/** Tests for xml:lang annotation selection and fallback. */
class AnnotationTextTest {

    private static final String LANG_XSD = "src/test/resources/reference/localized.core.v1.xsd";

    private static Schema load() {
        Schema schema = new Schema();
        schema.load(LANG_XSD);
        return schema;
    }

    private static XmlSchemaElement topLevel(Schema schema, String name) {
        for (SchemaComponent object : schema.getElements()) {
            if (object.name().equals(name) && object.tag() instanceof XmlSchemaElement element) {
                return element;
            }
        }
        throw new IllegalStateException("top-level element " + name + " not found");
    }

    private static XmlSchemaElement nested(Schema schema, String parent, String name) {
        XmlSchemaComplexType type = (XmlSchemaComplexType) topLevel(schema, parent).getSchemaType();
        XmlSchemaSequence sequence = (XmlSchemaSequence) type.getParticle();
        for (XmlSchemaSequenceMember member : sequence.getItems()) {
            if (member instanceof XmlSchemaElement element && element.getName().equals(name)) {
                return element;
            }
        }
        throw new IllegalStateException("nested element " + name + " not found");
    }

    @Test
    void selectsExactLanguage() {
        XmlSchemaElement greeting = topLevel(load(), "Greeting");
        assertEquals("English greeting.",
                AnnotationText.documentation(greeting.getAnnotation(), "en"));
        assertEquals("Русское приветствие.",
                AnnotationText.documentation(greeting.getAnnotation(), "ru"));
    }

    @Test
    void languageMatchIsCaseInsensitive() {
        XmlSchemaElement greeting = topLevel(load(), "Greeting");
        assertEquals("Русское приветствие.",
                AnnotationText.documentation(greeting.getAnnotation(), "RU"));
    }

    @Test
    void fallsBackToLanguageLessEntry() {
        XmlSchemaElement subject = nested(load(), "Greeting", "subject");
        assertEquals("Subject without a language.",
                AnnotationText.documentation(subject.getAnnotation(), "de"));
    }

    @Test
    void fallsBackToFirstEntryWhenNoLanguageLessEntry() {
        XmlSchemaElement body = nested(load(), "Greeting", "body");
        assertEquals("English body.",
                AnnotationText.documentation(body.getAnnotation(), "de"));
    }

    @Test
    void emptyLanguageConcatenatesAllEntries() {
        XmlSchemaElement greeting = topLevel(load(), "Greeting");
        assertEquals("English greeting. (en)\r\nРусское приветствие. (ru)",
                AnnotationText.documentation(greeting.getAnnotation(), ""));
    }

    @Test
    void nullLanguageSelectsTheDefaultEntry() {
        XmlSchemaElement greeting = topLevel(load(), "Greeting");
        assertEquals("English greeting.",
                AnnotationText.documentation(greeting.getAnnotation(), null));
        XmlSchemaElement subject = nested(load(), "Greeting", "subject");
        assertEquals("Subject without a language.",
                AnnotationText.documentation(subject.getAnnotation(), null));
    }

    @Test
    void documentationFallsBackToItsSource() {
        XmlSchemaElement sourced = topLevel(load(), "Sourced");
        assertEquals("http://example.org/doc",
                AnnotationText.documentation(sourced.getAnnotation(), "en"));
    }
}
