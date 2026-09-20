package org.jxsd.rendering;

import org.jxsd.model.DiagramNode;
/**
 * The reference page shape selected for a diagram root. Each constant is a
 * strategy that renders the root through the shared {@link PageRenderer}
 * collaborators, so adding a shape needs no dispatcher edit.
 */
enum PageShape {
    TYPE {
        @Override
        String render(PageRenderer renderer, int pageNumber, DiagramNode root) {
            return renderer.renderTypePage(pageNumber, root);
        }
    },
    CHAIN {
        @Override
        String render(PageRenderer renderer, int pageNumber, DiagramNode root) {
            return renderer.renderChainOrComposite(pageNumber, root, false);
        }
    },
    COMPOSITE {
        @Override
        String render(PageRenderer renderer, int pageNumber, DiagramNode root) {
            return renderer.renderChainOrComposite(pageNumber, root, true);
        }
    },
    NESTED_TYPE {
        @Override
        String render(PageRenderer renderer, int pageNumber, DiagramNode root) {
            return renderer.renderNestedType(pageNumber, root, false);
        }
    },
    NESTED_TYPE_ATTRIBUTES {
        @Override
        String render(PageRenderer renderer, int pageNumber, DiagramNode root) {
            return renderer.renderNestedType(pageNumber, root, true);
        }
    },
    EXTENSION {
        @Override
        String render(PageRenderer renderer, int pageNumber, DiagramNode root) {
            return renderer.renderExtensionPage(pageNumber, root);
        }
    },
    EXTENSION_NESTED {
        @Override
        String render(PageRenderer renderer, int pageNumber, DiagramNode root) {
            return renderer.renderNestedExtension(pageNumber, root);
        }
    },
    SIMPLE_ELEMENT {
        @Override
        String render(PageRenderer renderer, int pageNumber, DiagramNode root) {
            return renderer.renderSimpleElement(pageNumber, root);
        }
    },
    CONTEXT_SIMPLE {
        @Override
        String render(PageRenderer renderer, int pageNumber, DiagramNode root) {
            return renderer.renderContextSimple(pageNumber, root);
        }
    },
    CONTEXT {
        @Override
        String render(PageRenderer renderer, int pageNumber, DiagramNode root) {
            return renderer.renderContext(pageNumber, root);
        }
    },
    NESTED_CONTEXT {
        @Override
        String render(PageRenderer renderer, int pageNumber, DiagramNode root) {
            return renderer.renderNestedContext(pageNumber, root, false);
        }
    },
    NESTED_CONTEXT_ATTRIBUTES {
        @Override
        String render(PageRenderer renderer, int pageNumber, DiagramNode root) {
            return renderer.renderNestedContext(pageNumber, root, true);
        }
    },
    UNSUPPORTED {
        @Override
        String render(PageRenderer renderer, int pageNumber, DiagramNode root) {
            throw new IllegalStateException("Unsupported shape reached the renderer: " + root);
        }
    };

    abstract String render(PageRenderer renderer, int pageNumber, DiagramNode root);
}
