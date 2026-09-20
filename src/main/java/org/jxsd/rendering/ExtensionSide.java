package org.jxsd.rendering;

import java.util.List;

/**
 * One side (base or derived) of an {@code xs:extension} page: its content group
 * style, attribute tab and element children.
 */
public record ExtensionSide(GroupStyle style,
                            List<AttributeNode> attributes,
                            List<ElementView> children) {
}
