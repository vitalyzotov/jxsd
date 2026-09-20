package org.jxsd.model;

import org.jxsd.parsing.AnnotationText;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaObject;

/** Adapts an Apache XmlSchema component to the narrow {@link NodeSource} view. */
final class XmlSchemaSource implements NodeSource {

    private final XmlSchemaObject object;

    XmlSchemaSource(XmlSchemaObject object) {
        this.object = object;
    }

    @Override
    public String rawDocumentation(String language) {
        if (object instanceof XmlSchemaAnnotated annotated && annotated.getAnnotation() != null) {
            String text = AnnotationText.rawDocumentation(annotated.getAnnotation(), language);
            return text == null || text.isEmpty() ? null : text;
        }
        return null;
    }
}
