package org.jxsd.parsing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaAnyAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroup;
import org.apache.ws.commons.schema.XmlSchemaAttributeGroupRef;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaContentModel;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;

/**
 * Computes the attributes a component displays, already in render order: a
 * leading wildcard marker when the content permits one, then the attributes the
 * type inherits from its base, then the type's own attributes. Attribute groups
 * are flattened and attribute references are resolved to their target; cycles
 * are avoided by remembering the QNames already visited on the way down.
 */
public final class AttributeEnumerator {

    private static final SchemaAttribute WILDCARD = new SchemaAttribute("*", null);

    private AttributeEnumerator() {}

    /**
     * Returns the displayed attributes of an element, complex type or simple
     * type; anything else (and a typeless element) has none.
     */
    public static List<SchemaAttribute> attributesOf(Schema schema, XmlSchemaAnnotated annotated) {
        XmlSchemaComplexType type = complexTypeOf(annotated);
        return type == null ? List.of() : displayedAttributes(schema, type, new HashSet<>());
    }

    private static XmlSchemaComplexType complexTypeOf(XmlSchemaAnnotated annotated) {
        if (annotated instanceof XmlSchemaElement element) {
            return element.getSchemaType() instanceof XmlSchemaComplexType type ? type : null;
        }
        return annotated instanceof XmlSchemaComplexType type ? type : null;
    }

    private static List<SchemaAttribute> displayedAttributes(Schema schema,
            XmlSchemaComplexType type, Set<QName> visited) {
        if (!markVisited(type, visited)) {
            return List.of();
        }
        Content content = contentOf(type);
        List<SchemaAttribute> attributes = new ArrayList<>();
        if (content.anyAttribute() != null) {
            attributes.add(WILDCARD);
        }
        if (content.base() != null) {
            XmlSchemaType base = schema.findType(content.base());
            if (base instanceof XmlSchemaComplexType baseType) {
                attributes.addAll(displayedAttributes(schema, baseType, visited));
            }
        }
        if (!content.restricted()) {
            collectAttributes(schema, attributes, content.attributes(), visited);
        }
        return attributes;
    }

    /** Marks a named type as visited; an anonymous type is never tracked. */
    private static boolean markVisited(XmlSchemaComplexType type, Set<QName> visited) {
        QName name = type.getQName();
        return name == null || visited.add(name);
    }

    /**
     * Describes one complex type's contribution: the base it inherits from, its
     * own attribute declarations, a possible wildcard and whether it is a
     * restriction (whose own declarations only restate the base).
     */
    private static Content contentOf(XmlSchemaComplexType type) {
        XmlSchemaContentModel model = type.getContentModel();
        XmlSchemaContent content = model == null ? null : model.getContent();
        return switch (content) {
            case XmlSchemaComplexContentExtension extension ->
                    new Content(extension.getBaseTypeName(), extension.getAttributes(),
                            extension.getAnyAttribute(), false);
            case XmlSchemaSimpleContentExtension extension ->
                    new Content(extension.getBaseTypeName(), extension.getAttributes(),
                            extension.getAnyAttribute(), false);
            case XmlSchemaComplexContentRestriction restriction ->
                    new Content(restriction.getBaseTypeName(), restriction.getAttributes(),
                            restriction.getAnyAttribute(), true);
            case XmlSchemaSimpleContentRestriction restriction ->
                    new Content(restriction.getBaseTypeName(), restriction.getAttributes(),
                            restriction.getAnyAttribute(), true);
            case null, default ->
                    new Content(null, type.getAttributes(), type.getAnyAttribute(), false);
        };
    }

    private record Content(QName base, List<?> attributes,
            XmlSchemaAnyAttribute anyAttribute, boolean restricted) {}

    /** Flattens attribute declarations and attribute-group references in order. */
    private static void collectAttributes(Schema schema, List<SchemaAttribute> out,
            List<?> items, Set<QName> visited) {
        if (items == null) {
            return;
        }
        for (Object item : items) {
            if (item instanceof XmlSchemaAttribute attribute) {
                collectAttribute(out, attribute);
            } else if (item instanceof XmlSchemaAttributeGroupRef groupRef
                    && groupRef.getRef() != null && groupRef.getRef().getTarget() != null) {
                collectGroup(schema, out, groupRef.getRef().getTarget(), visited);
            }
        }
    }

    private static void collectGroup(Schema schema, List<SchemaAttribute> out,
            XmlSchemaAttributeGroup group, Set<QName> visited) {
        if (group.getQName() != null && !visited.add(group.getQName())) {
            return;
        }
        collectAttributes(schema, out, group.getAttributes(), visited);
    }

    /**
     * Adds a single attribute declaration, resolving {@code ref} to its target
     * and falling back to the reference's own name when the target is absent.
     */
    private static void collectAttribute(List<SchemaAttribute> out, XmlSchemaAttribute attribute) {
        if (attribute.isRef() && attribute.getRef() != null) {
            XmlSchemaAttribute resolved = attribute.getRef().getTarget();
            if (resolved != null) {
                collectAttribute(out, resolved);
                return;
            }
            QName reference = attribute.getRef().getTargetQName();
            if (reference != null) {
                out.add(new SchemaAttribute(reference.getLocalPart(), attribute));
                return;
            }
        }
        out.add(new SchemaAttribute(attribute.getName(), attribute));
    }
}
