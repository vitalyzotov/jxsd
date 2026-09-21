package org.jxsd.rendering;

import java.util.List;

/**
 * One side (base or derived) of an {@code xs:extension} page: its content group
 * style, attribute tab and element children. A {@code null} style means the side
 * has no content group (an extension base that only contributes attributes), so
 * no compositor is drawn.
 */
public record ExtensionSide(GroupStyle style,
                            List<AttributeNode> attributes,
                            List<ElementView> children) {
}
