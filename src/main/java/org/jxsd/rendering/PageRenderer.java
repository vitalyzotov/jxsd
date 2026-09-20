package org.jxsd.rendering;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jxsd.parsing.Schema;
import org.jxsd.parsing.SchemaAttribute;
import org.jxsd.model.Compositor;
import org.jxsd.model.ContentType;
import org.jxsd.model.DiagramContext;
import org.jxsd.model.DiagramNode;
import org.jxsd.model.ElementItem;
import org.jxsd.model.GroupRefItem;
import org.jxsd.model.ModelGroupItem;
import org.jxsd.model.Occurrence;
import org.jxsd.parsing.AnnotationText;
import org.apache.ws.commons.schema.XmlSchemaAttribute;

/**
 * Drives the reference-style renderers from a built {@link DiagramNode} tree.
 * {@link PageShapeResolver} chooses the page shape; this class maps the tree to
 * the renderer's view models and dispatches on the resolved shape. Unsupported
 * shapes yield an empty result.
 */
public final class PageRenderer {

    private final DiagramContext context;
    private final RenderOptions options;

    public PageRenderer() {
        this(DiagramContext.EMPTY, RenderOptions.DEFAULT);
    }

    public PageRenderer(RenderOptions options) {
        this(DiagramContext.EMPTY, options);
    }

    public PageRenderer(DiagramContext context, RenderOptions options) {
        this.context = context == null ? DiagramContext.EMPTY : context;
        this.options = options == null ? RenderOptions.DEFAULT : options;
    }

    /** Renders {@code root} as a reference page, or an empty result when unsupported. */
    public Optional<String> render(int pageNumber, DiagramNode root) {
        if (root == null) {
            return Optional.empty();
        }
        PageShape shape = PageShapeResolver.resolve(root);
        if (shape == PageShape.UNSUPPORTED) {
            return Optional.empty();
        }
        return Optional.of(shape.render(this, pageNumber, root));
    }

    // ------------------------------------------------------------------
    // Type roots
    // ------------------------------------------------------------------

    String renderTypePage(int pageNumber, DiagramNode root) {
        StringWriter writer = new StringWriter();
        new TypePage(writer, options)
                .render(pageNumber, rootName(root), rawDocumentation(root), attributes(root));
        return writer.toString();
    }

    String renderChainOrComposite(int pageNumber, DiagramNode root, boolean composite) {
        List<DiagramNode> groups = PageShapeResolver.contentGroups(root);
        DiagramNode group = groups.getLast();
        List<ElementView> children = elementChildren(group.children());
        StringWriter writer = new StringWriter();
        if (composite) {
            new CompositePage(writer, options).render(pageNumber, rootName(root), rawDocumentation(root),
                    groupStyle(group).compositor(), attributes(root), children);
        } else {
            new ChainPage(writer, options).render(pageNumber, rootName(root), rawDocumentation(root),
                    groupSpecs(groups), children);
        }
        return writer.toString();
    }

    String renderNestedType(int pageNumber, DiagramNode root, boolean withAttributes) {
        List<DiagramNode> groups = PageShapeResolver.contentGroups(root);
        NestedPage.Group nested = (NestedPage.Group) nestedNode(groups.getLast());
        StringWriter writer = new StringWriter();
        if (withAttributes) {
            new NestedPage(writer, options)
                    .render(pageNumber, rootName(root), rawDocumentation(root), attributes(root), nested);
        } else {
            new NestedPage(writer, options)
                    .render(pageNumber, rootName(root), rawDocumentation(root), nested);
        }
        return writer.toString();
    }

    String renderExtensionPage(int pageNumber, DiagramNode root) {
        DiagramNode base = PageShapeResolver.extensionBase(root);
        DiagramNode baseGroup = PageShapeResolver.contentGroups(base).getLast();
        DiagramNode derivedGroup = PageShapeResolver.lastDirectGroup(root);
        ExtensionSide baseSide = new ExtensionSide(groupStyle(baseGroup), attributes(base),
                elementChildren(baseGroup.children()));
        ExtensionSide derivedSide = derivedGroup == null
                ? new ExtensionSide(GroupStyle.compositor(Compositor.SEQUENCE), List.of(), List.of())
                : new ExtensionSide(groupStyle(derivedGroup), derivedAttributes(root, base),
                        elementChildren(derivedGroup.children()));
        StringWriter writer = new StringWriter();
        new ExtensionPage(writer, options).render(pageNumber, rootName(root), rawDocumentation(root),
                baseTypeLabel(base), baseSide, derivedSide);
        return writer.toString();
    }

    String renderNestedExtension(int pageNumber, DiagramNode root) {
        DiagramNode base = PageShapeResolver.extensionBase(root);
        DiagramNode baseGroup = PageShapeResolver.contentGroups(base).getLast();
        DiagramNode derivedGroup = PageShapeResolver.lastDirectGroup(root);
        StringWriter writer = new StringWriter();
        new NestedPage(writer, options).renderExtension(pageNumber, rootName(root), rawDocumentation(root),
                baseTypeLabel(base),
                attributes(base), (NestedPage.Group) nestedNode(baseGroup),
                derivedAttributes(root, base),
                derivedGroup == null ? null : (NestedPage.Group) nestedNode(derivedGroup));
        return writer.toString();
    }

    /** Attributes the derived type adds on top of the extension base. */
    private List<AttributeNode> derivedAttributes(DiagramNode root, DiagramNode base) {
        List<AttributeNode> baseAttributes = attributes(base);
        List<AttributeNode> result = new ArrayList<>();
        for (AttributeNode attribute : attributes(root)) {
            boolean inherited = baseAttributes.stream().anyMatch(candidate -> candidate.name().equals(attribute.name()));
            if (!inherited) {
                result.add(attribute);
            }
        }
        return result;
    }

    private String baseTypeLabel(DiagramNode base) {
        Schema schema = context.schema();
        String prefix = schema == null ? "" : schema.prefixFor(base.namespace());
        String name = base.name();
        return Labels.truncate((prefix.isEmpty() ? "" : prefix + ":") + name);
    }

    // ------------------------------------------------------------------
    // Element roots
    // ------------------------------------------------------------------

    String renderSimpleElement(int pageNumber, DiagramNode root) {
        StringWriter writer = new StringWriter();
        new SimpleElementPage(writer, options).render(pageNumber, rootName(root),
                rawDocumentation(root), root.occurrence());
        return writer.toString();
    }

    String renderContextSimple(int pageNumber, DiagramNode root) {
        StringWriter writer = new StringWriter();
        new ContextPage(writer, options)
                .render(pageNumber, elementSpec(root, rootName(root)),
                        displayName(root, typeName(root)), List.of(), List.of(), attributes(root));
        return writer.toString();
    }

    String renderContext(int pageNumber, DiagramNode root) {
        List<DiagramNode> groups = PageShapeResolver.contentGroups(root);
        List<DiagramNode> items = PageShapeResolver.elementContent(root, groups);
        List<GroupSpec> specs = groupSpecs(groups);
        List<ElementView> children = elementChildren(items);
        StringWriter writer = new StringWriter();
        new ContextPage(writer, options)
                .render(pageNumber, elementSpec(root, rootName(root)),
                        displayName(root, typeName(root)), specs, children, attributes(root));
        return writer.toString();
    }

    String renderNestedContext(int pageNumber, DiagramNode root, boolean withAttributes) {
        List<DiagramNode> groups = PageShapeResolver.contentGroups(root);
        List<DiagramNode> direct = PageShapeResolver.directGroups(root);
        NestedPage.Group nested = direct.size() == 1
                ? (NestedPage.Group) nestedNode(groups.getLast())
                : syntheticGroup(PageShapeResolver.elementContent(root, groups));
        StringWriter writer = new StringWriter();
        if (withAttributes) {
            new NestedPage(writer, options).renderElementContext(pageNumber,
                    elementSpec(root, rootName(root)), displayName(root, typeName(root)), attributes(root), nested);
        } else {
            new NestedPage(writer, options).renderElementContext(pageNumber,
                    elementSpec(root, rootName(root)), displayName(root, typeName(root)), nested);
        }
        return writer.toString();
    }

    /** The inline type title; an anonymous type has no name. */
    private static String typeName(DiagramNode root) {
        return root instanceof ElementItem element ? element.typeName() : "";
    }

    // ------------------------------------------------------------------
    // View models
    // ------------------------------------------------------------------

    private NestedPage.Node nestedNode(DiagramNode item) {
        if (item instanceof ElementItem element && !item.children().isEmpty()) {
            DiagramNode group = item.children().stream()
                    .filter(PageShapeResolver::isGroupNode)
                    .findFirst().orElse(null);
            if (group != null) {
                return new NestedPage.Expanded(elementView(item, displayName(item), true),
                        typeLabel(element), attributes(item), (NestedPage.Group) nestedNode(group));
            }
        }
        if (PageShapeResolver.isLeaf(item)) {
            return new NestedPage.Leaf(elementView(item, displayName(item), false));
        }
        List<NestedPage.Node> children = new ArrayList<>();
        for (DiagramNode child : item.children()) {
            children.add(nestedNode(child));
        }
        return new NestedPage.Group(groupStyle(item), item.occurrence(),
                rawDocumentation(item), children);
    }

    /** The drawing style of a group node: a named reference or a model group glyph. */
    private GroupStyle groupStyle(DiagramNode group) {
        if (group instanceof GroupRefItem) {
            return GroupStyle.named(displayName(group));
        }
        if (group instanceof ModelGroupItem item) {
            return GroupStyle.compositor(item.compositor());
        }
        return GroupStyle.compositor(Compositor.SEQUENCE);
    }

    /** Wraps flattened element children (extension roots) in a synthetic sequence. */
    private NestedPage.Group syntheticGroup(List<DiagramNode> items) {
        List<NestedPage.Node> children = new ArrayList<>();
        for (DiagramNode item : items) {
            children.add(nestedNode(item));
        }
        return new NestedPage.Group(Compositor.SEQUENCE, Occurrence.SINGLE, children);
    }

    /**
     * The prefixed name of an expanded element's named type (the container title),
     * or an empty string for an inline anonymous type. Uses the type's own
     * namespace rather than the element's for cross-namespace references.
     */
    private String typeLabel(ElementItem item) {
        String type = item.typeName();
        if (type.isEmpty()) {
            return "";
        }
        String namespace = item.typeNamespace().isEmpty() ? item.namespace() : item.typeNamespace();
        Schema schema = context.schema();
        String prefix = schema == null ? "" : schema.prefixFor(namespace);
        return Labels.truncate(prefix.isEmpty() ? type : prefix + ":" + type);
    }

    private List<GroupSpec> groupSpecs(List<DiagramNode> groups) {
        List<GroupSpec> specs = new ArrayList<>();
        for (DiagramNode group : groups) {
            specs.add(new GroupSpec(groupStyle(group), group.occurrence()));
        }
        return specs;
    }

    private List<ElementView> elementChildren(List<DiagramNode> items) {
        List<ElementView> children = new ArrayList<>();
        for (DiagramNode item : items) {
            children.add(elementView(item, displayName(item), false));
        }
        return children;
    }

    /** The referencing element of a context page, always shown expanded. */
    private ElementView elementSpec(DiagramNode item, String name) {
        return elementView(item, name, true);
    }

    /** Maps a model node and its prefixed label to the renderer's element view. */
    private ElementView elementView(DiagramNode item, String name, boolean expanded) {
        ContentType contentType = item.contentType();
        return new ElementView(name, rawDocumentation(item),
                ContentType.canExpand(contentType), expanded,
                ContentType.showsText(contentType), item.occurrence());
    }

    /** {@code root.name()} truncated to the reference label width. */
    private static String rootName(DiagramNode root) {
        return Labels.truncate(root.name());
    }

    /**
     * Prefixes a child element name with the schema prefix of its namespace, the
     * way XMLSpy labels properties ({@code cnf:title}). Root labels stay bare.
     */
    private String displayName(DiagramNode item) {
        return displayName(item, item.name());
    }

    private String displayName(DiagramNode item, String name) {
        Schema schema = context.schema();
        if (name == null || name.isEmpty() || schema == null) {
            return Labels.truncate(name);
        }
        String prefix = schema.prefixFor(item.namespace());
        return Labels.truncate(prefix.isEmpty() ? name : prefix + ":" + name);
    }

    private List<AttributeNode> attributes(DiagramNode item) {
        List<AttributeNode> nodes = new ArrayList<>();
        for (SchemaAttribute attribute : item.attributes()) {
            nodes.add(new AttributeNode(
                    Labels.truncate(attribute.name()),
                    attributeDocumentation(attribute)));
        }
        return nodes;
    }

    private String attributeDocumentation(SchemaAttribute attribute) {
        XmlSchemaAttribute annotated = attribute.tag();
        if (annotated != null && annotated.getAnnotation() != null) {
            return AnnotationText.documentation(annotated.getAnnotation(), context.language());
        }
        return null;
    }

    private String rawDocumentation(DiagramNode item) {
        if (!context.showDocumentation()) {
            return null;
        }
        return item.source().rawDocumentation(context.language());
    }
}
