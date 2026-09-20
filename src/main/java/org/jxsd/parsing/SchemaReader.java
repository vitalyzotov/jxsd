package org.jxsd.parsing;

import java.util.List;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.resolver.URIResolver;
import org.xml.sax.InputSource;

/**
 * Parses an XSD (and its dependencies) into an Apache XmlSchema 2 collection and
 * collects the top-level components the diagram works with.
 */
final class SchemaReader {

    private SchemaReader() {
    }

    /**
     * Reads {@code source} through {@code resolver} and appends the top-level
     * components to {@code out}.
     */
    static XmlSchemaCollection read(InputSource source, URIResolver resolver, List<SchemaComponent> out) {
        XmlSchemaCollection collection = new XmlSchemaCollection();
        collection.setBaseUri(source.getSystemId());
        collection.setSchemaResolver(resolver);
        collection.read(source);
        collectTopLevelComponents(collection, out);
        return collection;
    }

    private static void collectTopLevelComponents(XmlSchemaCollection collection, List<SchemaComponent> out) {
        for (XmlSchema schema : collection.getXmlSchemas()) {
            String namespace = schema.getTargetNamespace();
            if (Schema.XSD_NS.equals(namespace)) {
                continue;
            }
            for (XmlSchemaObject item : schema.getItems()) {
                ComponentKind type = componentType(item);
                if (type != null) {
                    out.add(new SchemaComponent(componentName(item), namespace, type, item));
                }
            }
        }
    }

    private static ComponentKind componentType(XmlSchemaObject item) {
        if (item instanceof XmlSchemaElement) { return ComponentKind.ELEMENT; }
        if (item instanceof XmlSchemaComplexType) { return ComponentKind.COMPLEX_TYPE; }
        if (item instanceof XmlSchemaSimpleType) { return ComponentKind.SIMPLE_TYPE; }
        if (item instanceof XmlSchemaGroup) { return ComponentKind.GROUP; }
        if (item instanceof XmlSchemaAttribute) { return ComponentKind.ATTRIBUTE; }
        if (item instanceof XmlSchemaAttributeGroup) { return ComponentKind.ATTRIBUTE_GROUP; }
        return null;
    }

    private static String componentName(XmlSchemaObject item) {
        if (item instanceof XmlSchemaElement e) { return e.getName(); }
        if (item instanceof XmlSchemaType t) { return t.getName(); }
        if (item instanceof XmlSchemaGroup g) { return g.getName(); }
        if (item instanceof XmlSchemaAttribute a) { return a.getName(); }
        if (item instanceof XmlSchemaAttributeGroup g) { return g.getName(); }
        return null;
    }
}
