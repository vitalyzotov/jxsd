package org.jxsd.model;

import java.util.List;
import java.util.Objects;

import javax.xml.namespace.QName;

import org.jxsd.parsing.AttributeEnumerator;
import org.jxsd.parsing.Schema;
import org.jxsd.parsing.SchemaAttribute;
import org.jxsd.parsing.SchemaComponent;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaContentModel;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaObject;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaType;

/**
 * Creates and attaches {@link DiagramNode} nodes from the Apache XmlSchema 2
 * object model. Expansion of the built tree lives in
 * {@link NodeExpander}.
 */
final class DiagramNodeFactory {

    private final Diagram diagram;

    DiagramNodeFactory(Diagram diagram) {
        this.diagram = diagram;
    }

    private Schema schema() {
        return diagram.getSchema();
    }

    private List<DiagramNode> rootElements() {
        return diagram.getRootElements();
    }

    DiagramNode addRoot(XmlSchemaObject childElement, String namespace) {
        return switch (childElement) {
            case XmlSchemaElement element -> appendElement(null, element, namespace);
            case XmlSchemaGroup group -> appendTopLevelGroup(group, namespace);
            case XmlSchemaComplexType complexType -> appendComplexType(null, complexType, namespace);
            case null, default -> null;
        };
    }

    // ------------------------------------------------------------------
    // Elements
    // ------------------------------------------------------------------

    ElementItem appendElement(DiagramNode parentNode, XmlSchemaElement childElement, String namespace) {
        if (childElement == null) {
            return null;
        }

        XmlSchemaElement referenceElement = null;
        XmlSchemaElement element = childElement;
        boolean reference = false;

        if (childElement.isRef() && childElement.getRef() != null) {
            reference = true;
            XmlSchemaElement target = childElement.getRef().getTarget();
            if (target != null) {
                referenceElement = childElement;
                element = target;
            }
        }

        ElementItem item = new ElementItem(elementName(element, childElement), namespace);
        ElementUse use = elementUse(reference, parentNode);
        item.setUse(use);
        item.setSource(element);
        item.setType(typeRefOf(element));

        XmlSchemaElement occurrenceElement = referenceElement != null ? referenceElement : childElement;
        item.setOccurrence(Occurrence.of(
                capLong(occurrenceElement.getMinOccurs()), capLong(occurrenceElement.getMaxOccurs())));

        item.setContentType(getChildrenInfo(element));
        item.setAttributes(attributesFor(element));

        attach(parentNode, item);

        // Only a global element declaration may be abstract and have substitutes.
        if (use == ElementUse.GLOBAL && element.isAbstract() && schema() != null) {
            for (SchemaComponent component : schema().getElements()) {
                if (component.tag() instanceof XmlSchemaElement candidate && candidate.getSubstitutionGroup() != null) {
                    QName substitutionGroup = candidate.getSubstitutionGroup();
                    if (Objects.equals(substitutionGroup.getNamespaceURI(), item.namespace())
                            && substitutionGroup.getLocalPart().equals(item.name())) {
                        ElementItem substitute = appendElement(parentNode, candidate, component.namespace());
                        if (substitute != null) {
                            substitute.setSubstitutionBase(item);
                            substitute.setUse(ElementUse.SUBSTITUTE);
                        }
                    }
                }
            }
        }

        return item;
    }

    private static ElementUse elementUse(boolean reference, DiagramNode parentNode) {
        if (reference) {
            return ElementUse.REFERENCE;
        }
        return parentNode == null ? ElementUse.GLOBAL : ElementUse.LOCAL;
    }

    private static String elementName(XmlSchemaElement element, XmlSchemaElement original) {
        if (element.getName() != null) {
            return element.getName();
        }
        if (original.getRef() != null && original.getRef().getTargetQName() != null) {
            return original.getRef().getTargetQName().getLocalPart();
        }
        return "";
    }

    /** The element's {@code {type definition}}: a named reference or an inline type. */
    private static TypeRef typeRefOf(XmlSchemaElement element) {
        QName schemaTypeName = element.getSchemaTypeName();
        if (schemaTypeName != null) {
            return new TypeRef.Named(schemaTypeName);
        }
        XmlSchemaType schemaType = element.getSchemaType();
        if (schemaType == null) {
            return null;
        }
        if (schemaType.getName() != null) {
            return new TypeRef.Named(new QName("", schemaType.getName()));
        }
        return new TypeRef.Anonymous(schemaType);
    }

    // ------------------------------------------------------------------
    // Groups / compositors
    // ------------------------------------------------------------------

    private ModelGroupItem appendTopLevelGroup(XmlSchemaGroup group, String namespace) {
        return appendModelGroup(null, compositorOf(group.getParticle()), group, namespace, "1", "1");
    }

    /** An anonymous XSD model group ({@code xs:sequence}/{@code xs:choice}/{@code xs:all}). */
    ModelGroupItem appendModelGroup(DiagramNode parentNode, Compositor type,
            XmlSchemaObject tabSchema, String namespace, String minOccurs, String maxOccurs) {
        ModelGroupItem item = new ModelGroupItem("", namespace);
        item.setCompositor(type);
        item.setSource(tabSchema);
        item.setOccurrence(Occurrence.of(
                parseInt(minOccurs == null ? "1" : minOccurs), parseInt(maxOccurs == null ? "1" : maxOccurs)));
        item.setAttributes(attributesFor(tabSchema));
        attach(parentNode, item);
        return item;
    }

    /** An XSD group reference particle ({@code xs:group ref}), resolved to its definition. */
    GroupRefItem appendGroupRef(DiagramNode parentNode, XmlSchemaObject tabSchema,
            XmlSchemaGroup resolvedGroup, String name, String namespace, String minOccurs, String maxOccurs) {
        XmlSchemaObject source = resolvedGroup != null ? resolvedGroup : tabSchema;
        GroupRefItem item = new GroupRefItem(name, namespace);
        item.setSource(source);
        item.setOccurrence(Occurrence.of(
                parseInt(minOccurs == null ? "1" : minOccurs), parseInt(maxOccurs == null ? "1" : maxOccurs)));
        item.setAttributes(attributesFor(source));
        attach(parentNode, item);
        return item;
    }

    /** The compositor of a model group particle; {@code null} (empty group) is a sequence. */
    static Compositor compositorOf(XmlSchemaParticle particle) {
        if (particle == null || particle instanceof XmlSchemaSequence) {
            return Compositor.SEQUENCE;
        }
        if (particle instanceof XmlSchemaAll) {
            return Compositor.ALL;
        }
        if (particle instanceof XmlSchemaChoice) {
            return Compositor.CHOICE;
        }
        throw new IllegalArgumentException(
                "Unexpected model group particle: " + particle.getClass().getName());
    }

    WildcardItem appendWildcard(DiagramNode parentNode, XmlSchemaAny childElement, String namespace) {
        if (childElement == null) {
            WildcardItem fallback = new WildcardItem("##any", namespace);
            fallback.setOccurrence(Occurrence.unbounded(0));
            attach(parentNode, fallback);
            return fallback;
        }
        WildcardItem item = new WildcardItem(childElement.getNamespace(), namespace);
        item.setSource(childElement);
        item.setOccurrence(Occurrence.of(capLong(childElement.getMinOccurs()), capLong(childElement.getMaxOccurs())));
        item.setAttributes(attributesFor(childElement));
        attach(parentNode, item);
        return item;
    }

    // ------------------------------------------------------------------
    // Complex types
    // ------------------------------------------------------------------

    TypeItem appendComplexType(DiagramNode parentNode, XmlSchemaComplexType childElement, String namespace) {
        if (childElement == null) {
            return null;
        }
        TypeItem item = new TypeItem(childElement.getName() != null ? childElement.getName() : "", namespace);
        item.setSource(childElement);
        item.setOccurrence(Occurrence.SINGLE);
        item.setContentType(getChildrenInfo(childElement));
        item.setAttributes(attributesFor(childElement));
        attach(parentNode, item);
        return item;
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private ContentType getChildrenInfo(XmlSchemaElement element) {
        if (element.getSchemaType() instanceof XmlSchemaComplexType complexType) {
            return getChildrenInfo(complexType);
        }
        return ContentType.SIMPLE;
    }

    private ContentType getChildrenInfo(XmlSchemaComplexType complexType) {
        boolean simpleContent = complexType.isMixed();
        XmlSchemaContentModel contentModel = complexType.getContentModel();
        XmlSchemaContent content = contentModel == null ? null : contentModel.getContent();
        XmlSchemaParticleInfo particleInfo = particleInfo(contentModel, content, complexType);
        if (particleInfo == null) {
            return ContentType.SIMPLE;
        }
        if (particleInfo.borrowedBase() != null) {
            XmlSchemaType base = schema() == null ? null : schema().findType(particleInfo.borrowedBase());
            if (base instanceof XmlSchemaComplexType baseComplexType) {
                return getChildrenInfo(baseComplexType);
            }
        }
        return ContentType.of(particleInfo.hasChildren(), simpleContent);
    }

    /** Content of a complex type: whether it has a particle and, if inherited, from which base. */
    private record XmlSchemaParticleInfo(boolean hasChildren, QName borrowedBase) {}

    /**
     * Mirrors {@code NodeExpander.expandComplexType}: a restriction takes its
     * content from the base type, an extension without a particle only inherits it.
     */
    private XmlSchemaParticleInfo particleInfo(XmlSchemaContentModel contentModel,
            XmlSchemaContent content, XmlSchemaComplexType complexType) {
        return switch (content) {
            case XmlSchemaComplexContentExtension extension ->
                    new XmlSchemaParticleInfo(extension.getParticle() != null,
                            extension.getParticle() == null ? extension.getBaseTypeName() : null);
            case XmlSchemaComplexContentRestriction restriction ->
                    new XmlSchemaParticleInfo(restriction.getParticle() != null, restriction.getBaseTypeName());
            case null, default -> {
                if (contentModel instanceof XmlSchemaSimpleContent) {
                    yield null;
                }
                yield complexType.getParticle() != null
                        ? new XmlSchemaParticleInfo(true, null) : new XmlSchemaParticleInfo(false, null);
            }
        };
    }

    private void attach(DiagramNode parentNode, DiagramNode childNode) {
        if (parentNode == null) {
            rootElements().add(childNode);
        } else {
            parentNode.attach(childNode);
        }
    }

    /** Computes the displayed attributes of a component once, at build time. */
    private List<SchemaAttribute> attributesFor(XmlSchemaObject tab) {
        if (schema() == null || !(tab instanceof XmlSchemaAnnotated annotated)) {
            return List.of();
        }
        return List.copyOf(AttributeEnumerator.attributesOf(schema(), annotated));
    }

    /** Parses an occurrence literal, mapping an unparsable value to unbounded. */
    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return Occurrence.UNBOUNDED;
        }
    }

    /** Caps a long occurrence, mapping anything at/over {@code Integer.MAX_VALUE} to unbounded. */
    private static int capLong(long value) {
        return value >= Integer.MAX_VALUE ? Occurrence.UNBOUNDED : (int) value;
    }
}
