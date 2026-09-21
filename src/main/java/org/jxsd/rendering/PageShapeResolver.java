package org.jxsd.rendering;

import java.util.ArrayList;
import java.util.List;
import org.jxsd.model.DiagramNode;
import org.jxsd.model.ElementItem;
import org.jxsd.model.GroupRefItem;
import org.jxsd.model.ModelGroupItem;
import org.jxsd.model.TypeItem;
import org.jxsd.model.WildcardItem;

/**
 * Decides which reference page shape a built {@link DiagramNode} tree maps to,
 * keeping the shape rules out of the rendering code. Also hosts the structural
 * queries (content groups, leaf/expanded tests) the page renderers share.
 */
final class PageShapeResolver {

    private PageShapeResolver() {
    }

    static PageShape resolve(DiagramNode root) {
        if (root instanceof TypeItem) {
            return resolveType(root);
        }
        if (root instanceof ElementItem element) {
            return resolveElement(element);
        }
        return PageShape.UNSUPPORTED;
    }

    private static PageShape resolveType(DiagramNode root) {
        DiagramNode base = extensionBase(root);
        if (base != null) {
            return resolveExtension(root, base);
        }
        List<DiagramNode> groups = contentGroups(root);
        if (groups.isEmpty()) {
            return PageShape.TYPE;
        }
        List<DiagramNode> items = groups.getLast().children();
        if (hasExpandableContent(items)) {
            if (!root.attributes().isEmpty() && !hasExpanded(items)) {
                return PageShape.UNSUPPORTED;
            }
            return root.attributes().isEmpty()
                    ? PageShape.NESTED_TYPE : PageShape.NESTED_TYPE_ATTRIBUTES;
        }
        if (items.stream().anyMatch(item -> !isLeaf(item))) {
            return PageShape.UNSUPPORTED;
        }
        return root.attributes().isEmpty() ? PageShape.CHAIN : PageShape.COMPOSITE;
    }

    private static PageShape resolveExtension(DiagramNode root, DiagramNode base) {
        List<DiagramNode> baseGroups = contentGroups(base);
        if (baseGroups.isEmpty() && base.attributes().isEmpty()) {
            return PageShape.UNSUPPORTED;
        }
        List<DiagramNode> baseChildren = baseGroups.isEmpty()
                ? List.of()
                : baseGroups.getLast().children();
        DiagramNode derivedGroup = lastDirectGroup(root);
        if (hasExpanded(baseChildren)
                || (derivedGroup != null && hasExpanded(derivedGroup.children()))) {
            return PageShape.EXTENSION_NESTED;
        }
        if (!allElements(baseChildren)) {
            return PageShape.UNSUPPORTED;
        }
        if (derivedGroup != null && !allElements(derivedGroup.children())) {
            return PageShape.UNSUPPORTED;
        }
        return PageShape.EXTENSION;
    }

    private static PageShape resolveElement(ElementItem root) {
        List<DiagramNode> groups = contentGroups(root);
        if (groups.isEmpty()) {
            String simpleTypeName = root.typeName();
            if (!root.attributes().isEmpty() && !simpleTypeName.isEmpty()) {
                return PageShape.CONTEXT_SIMPLE;
            }
            return PageShape.SIMPLE_ELEMENT;
        }
        List<DiagramNode> items = elementContent(root, groups);
        if (hasExpandableContent(items)) {
            if (!root.attributes().isEmpty() && !hasExpanded(items)) {
                return PageShape.UNSUPPORTED;
            }
            return root.attributes().isEmpty()
                    ? PageShape.NESTED_CONTEXT : PageShape.NESTED_CONTEXT_ATTRIBUTES;
        }
        if (items.stream().anyMatch(item -> !isLeaf(item))) {
            return PageShape.UNSUPPORTED;
        }
        return PageShape.CONTEXT;
    }

    // ------------------------------------------------------------------
    // Structural queries shared with the renderers
    // ------------------------------------------------------------------

    /** The elements an element root renders, flattening extension group levels. */
    static List<DiagramNode> elementContent(DiagramNode root, List<DiagramNode> groups) {
        List<DiagramNode> direct = directGroups(root);
        if (direct.size() == 1) {
            return groups.getLast().children();
        }
        List<DiagramNode> items = new ArrayList<>();
        for (DiagramNode group : direct) {
            collectElements(group, items);
        }
        return items;
    }

    static DiagramNode extensionBase(DiagramNode root) {
        for (DiagramNode child : root.children()) {
            if (child instanceof TypeItem type && type.isExtensionBase()) {
                return child;
            }
        }
        return null;
    }

    static boolean allElements(List<DiagramNode> items) {
        return items.stream().allMatch(PageShapeResolver::isLeaf);
    }

    /** An element, or a wildcard ({@code xs:any}) particle. */
    static boolean isLeaf(DiagramNode item) {
        if (item instanceof ElementItem) {
            return item.children().isEmpty();
        }
        return item instanceof WildcardItem;
    }

    /** True when a content element actually revealed its own children ({@code -e >= 2}). */
    static boolean hasExpanded(List<DiagramNode> items) {
        return items.stream().anyMatch(item -> item instanceof ElementItem && !item.children().isEmpty());
    }

    /**
     * True when the content holds a nested group or an element with revealed
     * children. A group node is always expandable: {@code ModelGroupItem} and
     * {@code GroupRefItem} are {@link org.jxsd.model.ContentType#ELEMENT_ONLY}
     * by construction.
     */
    static boolean hasExpandableContent(List<DiagramNode> items) {
        return items.stream().anyMatch(item -> isGroupNode(item)
                || (item instanceof ElementItem && !item.children().isEmpty()));
    }

    /** A model group or a group reference; wildcards are leaves, not groups. */
    static boolean isGroupNode(DiagramNode node) {
        return node instanceof ModelGroupItem || node instanceof GroupRefItem;
    }

    static DiagramNode lastDirectGroup(DiagramNode root) {
        DiagramNode result = null;
        for (DiagramNode child : root.children()) {
            if (isGroupNode(child)) {
                result = child;
            }
        }
        return result;
    }

    /** All direct group children; an extension expands into one group per level. */
    static List<DiagramNode> directGroups(DiagramNode root) {
        List<DiagramNode> groups = new ArrayList<>();
        for (DiagramNode child : root.children()) {
            if (isGroupNode(child)) {
                groups.add(child);
            }
        }
        return groups;
    }

    /** Flattens a chain of single-group compositors down to the element list. */
    private static void collectElements(DiagramNode group, List<DiagramNode> out) {
        List<DiagramNode> children = group.children();
        if (children.size() == 1 && isGroupNode(children.getFirst())) {
            collectElements(children.getFirst(), out);
        } else {
            out.addAll(children);
        }
    }

    static DiagramNode contentGroup(DiagramNode root) {
        for (DiagramNode child : root.children()) {
            if (isGroupNode(child)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Follows a chain of single-group compositors ({@code type -> sequence ->
     * sequence -> ...}) down to the group that directly holds the elements.
     */
    static List<DiagramNode> contentGroups(DiagramNode root) {
        List<DiagramNode> groups = new ArrayList<>();
        DiagramNode group = contentGroup(root);
        while (group != null) {
            groups.add(group);
            List<DiagramNode> children = group.children();
            if (children.size() == 1 && isGroupNode(children.getFirst())) {
                group = children.getFirst();
            } else {
                break;
            }
        }
        return groups;
    }
}
