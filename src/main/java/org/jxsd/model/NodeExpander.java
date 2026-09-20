package org.jxsd.model;

import org.jxsd.parsing.Schema;
import org.apache.ws.commons.schema.XmlSchemaAll;
import org.apache.ws.commons.schema.XmlSchemaAllMember;
import org.apache.ws.commons.schema.XmlSchemaAnnotated;
import org.apache.ws.commons.schema.XmlSchemaAny;
import org.apache.ws.commons.schema.XmlSchemaChoice;
import org.apache.ws.commons.schema.XmlSchemaChoiceMember;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaContentModel;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaGroupRef;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSimpleContent;
import org.apache.ws.commons.schema.XmlSchemaType;

/**
 * The primitive building blocks a {@link DiagramNode} uses to reveal its own
 * content: it walks the resolved particles, compositors and complex-type
 * content, asking {@link DiagramNodeFactory} to create the newly revealed
 * nodes. The node kind decides <em>what</em> to reveal through
 * {@link DiagramNode#applyContent}; this collaborator owns the Apache
 * XmlSchema 2 traversal that knows <em>how</em>.
 */
final class NodeExpander {

    private final Diagram diagram;
    private final DiagramNodeFactory factory;

    NodeExpander(Diagram diagram, DiagramNodeFactory factory) {
        this.diagram = diagram;
        this.factory = factory;
    }

    Schema schema() {
        return diagram.getSchema();
    }

    /** Reveals the content of an element node: its resolved complex type. */
    void expandElement(ElementItem element) {
        XmlSchemaType type = elementType(element);
        if (type instanceof XmlSchemaComplexType complexType) {
            expandComplexType(element, complexType);
        }
    }

    /** Reveals the content of a complex-type node. */
    void expandType(TypeItem type) {
        if (type.parsed() instanceof XmlSchemaComplexType complexType) {
            expandComplexType(type, complexType);
        }
    }

    /** Reveals a model group or group reference node from its resolved particle. */
    void expandParticleContent(DiagramNode parent) {
        XmlSchemaParticle particle = switch (parent.parsed()) {
            case XmlSchemaGroup group -> group.getParticle();
            case XmlSchemaParticle p -> p;
            case null, default -> null;
        };
        if (particle == null) {
            factory.appendWildcard(parent, null, parent.namespace());
        } else {
            expandGroup(parent, particle);
        }
    }

    /**
     * An element reference takes its type from the referenced declaration; a global,
     * local or substitute declaration takes the type recorded in its {@link TypeRef}.
     */
    private XmlSchemaType elementType(ElementItem element) {
        if (element.use() == ElementUse.REFERENCE) {
            return declarationType(element);
        }
        return switch (element.type()) {
            case TypeRef.Named named -> {
                XmlSchemaType resolved = schema() == null ? null : schema().findType(named.name());
                yield resolved != null ? resolved : declarationType(element);
            }
            case TypeRef.Anonymous anonymous -> anonymous.definition();
            case null -> declarationType(element);
        };
    }

    private static XmlSchemaType declarationType(DiagramNode item) {
        return item.parsed() instanceof XmlSchemaElement element ? element.getSchemaType() : null;
    }

    private void expandComplexType(DiagramNode parent, XmlSchemaComplexType complexType) {
        parent.markExpanded();
        XmlSchemaContentModel contentModel = complexType.getContentModel();
        XmlSchemaContent content = contentModel == null ? null : contentModel.getContent();
        if (contentModel instanceof XmlSchemaComplexContent) {
            if (content instanceof XmlSchemaComplexContentExtension extension) {
                expandExtension(parent, extension);
            } else if (content instanceof XmlSchemaComplexContentRestriction restriction) {
                expandRestriction(parent, restriction);
            }
        } else if (contentModel instanceof XmlSchemaSimpleContent) {
            // simple content has no child elements
        } else if (complexType.getParticle() != null) {
            addCompositors(parent, DiagramNodeFactory.compositorOf(complexType.getParticle()),
                    complexType.getParticle(), parent.namespace());
        }
    }

    private void expandExtension(DiagramNode parent, XmlSchemaComplexContentExtension extension) {
        XmlSchemaType base = schema() == null ? null : schema().findType(extension.getBaseTypeName());
        String baseNamespace = extension.getBaseTypeName() == null ? parent.namespace()
                : extension.getBaseTypeName().getNamespaceURI();
        if (base instanceof XmlSchemaComplexType baseComplexType && parent instanceof TypeItem) {
            TypeItem baseItem = factory.appendComplexType(parent, baseComplexType, baseNamespace);
            baseItem.setExtensionBase(true);
            expandComplexType(baseItem, baseComplexType);
        } else if (base != null) {
            expandAnnotated(parent, base, baseNamespace);
        }
        if (extension.getParticle() != null) {
            addCompositors(parent, DiagramNodeFactory.compositorOf(extension.getParticle()),
                    extension.getParticle(), parent.namespace());
        }
    }

    private void expandRestriction(DiagramNode parent, XmlSchemaComplexContentRestriction restriction) {
        if (restriction.getParticle() != null) {
            addCompositors(parent, DiagramNodeFactory.compositorOf(restriction.getParticle()),
                    restriction.getParticle(), parent.namespace());
            return;
        }
        XmlSchemaType base = schema() == null ? null : schema().findType(restriction.getBaseTypeName());
        if (base != null) {
            expandAnnotated(parent, base,
                    restriction.getBaseTypeName() == null ? parent.namespace()
                            : restriction.getBaseTypeName().getNamespaceURI());
        }
    }

    private void expandGroup(DiagramNode parentGroup, XmlSchemaParticle particle) {
        parentGroup.markExpanded();
        XmlSchemaParticle effective = particle;
        if (effective instanceof XmlSchemaGroupRef groupRef) {
            XmlSchemaGroup target = schema() == null ? null : schema().findGroup(groupRef.getRefName());
            effective = target == null ? null : target.getParticle();
        }
        if (effective == null) {
            factory.appendWildcard(parentGroup, null, parentGroup.namespace());
            return;
        }
        switch (effective) {
            case XmlSchemaSequence sequence -> {
                for (XmlSchemaSequenceMember member : sequence.getItems()) {
                    expandParticle(parentGroup, member);
                }
            }
            case XmlSchemaChoice choice -> {
                for (XmlSchemaChoiceMember member : choice.getItems()) {
                    expandParticle(parentGroup, member);
                }
            }
            case XmlSchemaAll all -> {
                for (XmlSchemaAllMember member : all.getItems()) {
                    expandParticle(parentGroup, member);
                }
            }
            case XmlSchemaElement element -> factory.appendElement(parentGroup, element, parentGroup.namespace());
            case XmlSchemaAny any -> factory.appendWildcard(parentGroup, any, parentGroup.namespace());
            case null, default -> {
            }
        }
    }

    private void expandParticle(DiagramNode parent, Object member) {
        switch (member) {
            case XmlSchemaElement element -> factory.appendElement(parent, element, parent.namespace());
            case XmlSchemaAny any -> factory.appendWildcard(parent, any, parent.namespace());
            case XmlSchemaGroupRef groupRef -> {
                XmlSchemaGroup target = schema() == null ? null : schema().findGroup(groupRef.getRefName());
                String refName = groupRef.getRefName() == null ? "" : groupRef.getRefName().getLocalPart();
                String refNamespace = groupRef.getRefName() == null ? "" : groupRef.getRefName().getNamespaceURI();
                GroupRefItem groupItem = factory.appendGroupRef(parent, groupRef, target,
                        refName, refNamespace,
                        Long.toString(groupRef.getMinOccurs()), Long.toString(groupRef.getMaxOccurs()));
                expandGroup(groupItem, groupRef);
            }
            case XmlSchemaSequence sequence -> {
                ModelGroupItem groupItem = factory.appendModelGroup(parent, Compositor.SEQUENCE, sequence,
                        parent.namespace(),
                        Long.toString(sequence.getMinOccurs()), Long.toString(sequence.getMaxOccurs()));
                expandGroup(groupItem, sequence);
            }
            case XmlSchemaChoice choice -> {
                ModelGroupItem groupItem = factory.appendModelGroup(parent, Compositor.CHOICE, choice,
                        parent.namespace(),
                        Long.toString(choice.getMinOccurs()), Long.toString(choice.getMaxOccurs()));
                expandGroup(groupItem, choice);
            }
            case XmlSchemaAll all -> {
                ModelGroupItem groupItem = factory.appendModelGroup(parent, Compositor.ALL, all,
                        parent.namespace(),
                        Long.toString(all.getMinOccurs()), Long.toString(all.getMaxOccurs()));
                expandGroup(groupItem, all);
            }
            case null, default -> {
            }
        }
    }

    private ModelGroupItem addCompositors(DiagramNode parent, Compositor type,
            XmlSchemaParticle particle, String namespace) {
        ModelGroupItem item = factory.appendModelGroup(parent, type, particle, namespace,
                Long.toString(particle.getMinOccurs()), Long.toString(particle.getMaxOccurs()));
        expandGroup(item, particle);
        return item;
    }

    private void expandAnnotated(DiagramNode parent, XmlSchemaAnnotated annotated, String namespace) {
        switch (annotated) {
            case XmlSchemaElement element -> factory.appendElement(parent, element, namespace);
            case XmlSchemaGroup group -> {
                ModelGroupItem groupItem = factory.appendModelGroup(parent,
                        DiagramNodeFactory.compositorOf(group.getParticle()), group, namespace, "1", "1");
                if (group.getParticle() != null) {
                    expandGroup(groupItem, group.getParticle());
                } else {
                    factory.appendWildcard(groupItem, null, groupItem.namespace());
                }
            }
            case XmlSchemaComplexType complexType -> expandComplexType(parent, complexType);
            case null, default -> {
            }
        }
    }
}
