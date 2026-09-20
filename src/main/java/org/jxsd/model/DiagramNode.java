package org.jxsd.model;

import java.util.ArrayList;
import java.util.List;

import org.jxsd.parsing.SchemaAttribute;
import org.apache.ws.commons.schema.XmlSchemaObject;

/**
 * A node in the diagram tree: an element particle, a complex type definition, an
 * anonymous model group, a group reference or a wildcard. The sealed hierarchy
 * makes type-specific state reachable only on the matching node kind and keeps
 * the shared structure in one place.
 */
public abstract sealed class DiagramNode
        permits ElementItem, TypeItem, ModelGroupItem, GroupRefItem, WildcardItem {

    private final String name;
    private final String namespace;

    private Occurrence occurrence = Occurrence.SINGLE;
    private ContentType contentType;
    private List<SchemaAttribute> attributes = List.of();
    private XmlSchemaObject parsed;
    private NodeSource source = NodeSource.NONE;
    private boolean expanded;

    private final List<DiagramNode> children = new ArrayList<>();

    DiagramNode(String name, String namespace) {
        this.name = name == null ? "" : name;
        this.namespace = namespace == null ? "" : namespace;
    }

    public final String name() { return name; }

    public final String namespace() { return namespace; }

    public final Occurrence occurrence() { return occurrence; }
    final void setOccurrence(Occurrence value) { occurrence = value; }

    /**
     * The node's XSD content kind or {@code null} when none is set: an element/type
     * carries its declared content, a model group or group reference is
     * {@link ContentType#ELEMENT_ONLY} and a wildcard stays {@code null}.
     * Callers must treat the value as nullable; {@link ContentType#canExpand} and
     * {@link ContentType#showsText} answer the two questions the renderer asks.
     */
    public final ContentType contentType() { return contentType; }
    final void setContentType(ContentType value) { contentType = value; }

    /** The displayed attributes computed by the tree builder; never {@code null}. */
    public final List<SchemaAttribute> attributes() { return attributes; }
    final void setAttributes(List<SchemaAttribute> value) {
        attributes = value == null ? List.of() : List.copyOf(value);
    }

    /** The renderer-facing source view (raw documentation only). */
    public final NodeSource source() { return source; }

    /** The parsed Apache component; package-private for the tree expander. */
    final XmlSchemaObject parsed() { return parsed; }

    final void setSource(XmlSchemaObject value) {
        parsed = value;
        source = value == null ? NodeSource.NONE : new XmlSchemaSource(value);
    }

    /** True once this node's own content has been revealed. */
    final boolean isExpanded() { return expanded; }

    /** Marks this node as revealed; a reveal that attaches no child relies on it. */
    final void markExpanded() { expanded = true; }

    /**
     * True while the node is still part of the expansion frontier: its content
     * can hold child nodes and has not been revealed yet.
     */
    final boolean awaitsExpansion() {
        return !expanded && ContentType.canExpand(contentType);
    }

    /**
     * Reveals this node's own content exactly once. A node is never revealed
     * twice.
     */
    final void expandContent(NodeExpander expander) {
        markExpanded();
        applyContent(expander);
    }

    /**
     * Reveals the content the node kind owns; called at most once per node.
     * A node kind without expandable content leaves this a no-op.
     */
    abstract void applyContent(NodeExpander expander);

    /**
     * The not-yet-revealed nodes reachable from this node, itself included while
     * it still waits. Descends only through revealed nodes, since an unrevealed
     * node has no children.
     */
    final List<DiagramNode> unexpandedNodes() {
        List<DiagramNode> frontier = new ArrayList<>();
        collectUnexpanded(frontier);
        return frontier;
    }

    private void collectUnexpanded(List<DiagramNode> frontier) {
        if (!expanded) {
            frontier.add(this);
            return;
        }
        for (DiagramNode child : children) {
            child.collectUnexpanded(frontier);
        }
    }

    /** The child nodes; only the tree builder may attach to this list. */
    public final List<DiagramNode> children() { return children; }

    /**
     * Attaches a revealed child. A node that gained children has ipso facto
     * revealed content, so it leaves the expansion frontier; a reveal that
     * yields no child must {@link #markExpanded() mark itself}.
     */
    final void attach(DiagramNode child) {
        children.add(child);
        expanded = true;
    }
}
