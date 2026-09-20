package org.jxsd.model;

import java.util.ArrayList;
import java.util.List;

import org.jxsd.parsing.Schema;
import org.apache.ws.commons.schema.XmlSchemaObject;

/**
 * Holds the rendering options, the schema registry and the root items. Node
 * creation lives in {@link DiagramNodeFactory}; each node reveals its own
 * content through {@link DiagramNode#expandContent}, using the traversal
 * primitives of {@link NodeExpander}.
 */
public final class Diagram {

    private boolean showDocumentation;
    private String language;

    private Schema schema;
    private final List<DiagramNode> rootElements = new ArrayList<>();

    private final DiagramNodeFactory nodeFactory;
    private final NodeExpander nodeExpander;

    public Diagram() {
        nodeFactory = new DiagramNodeFactory(this);
        nodeExpander = new NodeExpander(this, nodeFactory);
    }

    public void setShowDocumentation(boolean value) { showDocumentation = value; }

    public void setLanguage(String value) { language = value; }

    public Schema getSchema() { return schema; }
    public void setSchema(Schema value) { schema = value; }

    public List<DiagramNode> getRootElements() { return rootElements; }

    /** Immutable snapshot of the schema and display options the tree is built with. */
    public DiagramContext context() {
        return new DiagramContext(schema, showDocumentation, language);
    }

    public DiagramNode addRoot(XmlSchemaObject childElement, String namespace) {
        return nodeFactory.addRoot(childElement, namespace);
    }

    /** Expands the tree by one level and returns how many levels it actually grew. */
    public int expand() {
        return expand(1);
    }

    /**
     * Expands the tree by up to {@code levels} levels.
     *
     * <p>One level is one breadth-first pass over the unexpanded frontier: every
     * node that can reveal content does so, and the next pass picks up the nodes
     * this one revealed. The frontier is recomputed from the roots on every call,
     * so successive {@code expand} calls continue where the previous one stopped.
     * The number of passes is bounded by {@code levels} and by the tree itself. A
     * node whose content cannot yield children (simple content, an empty complex
     * type) is not expanded and does not count as a level.
     *
     * @return the number of levels actually grown, which is below {@code levels}
     *         once the tree can grow no further
     * @throws IllegalArgumentException if {@code levels} is negative
     */
    public int expand(int levels) {
        if (levels < 0) {
            throw new IllegalArgumentException("The expand level must not be negative.");
        }
        int grown = 0;
        while (grown < levels) {
            boolean revealed = expandFrontier(frontier());
            if (!revealed) {
                break;
            }
            grown++;
        }
        return grown;
    }

    /** All not-yet-revealed nodes reachable from the roots, in document order. */
    private List<DiagramNode> frontier() {
        List<DiagramNode> frontier = new ArrayList<>();
        for (DiagramNode root : rootElements) {
            frontier.addAll(root.unexpandedNodes());
        }
        return frontier;
    }

    /** Reveals every waiting node of the frontier; answers whether any was revealed. */
    private boolean expandFrontier(List<DiagramNode> frontier) {
        boolean revealed = false;
        for (DiagramNode node : frontier) {
            if (node.awaitsExpansion()) {
                node.expandContent(nodeExpander);
                revealed = true;
            }
        }
        return revealed;
    }
}
