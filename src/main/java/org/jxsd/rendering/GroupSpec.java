package org.jxsd.rendering;

import org.jxsd.model.Compositor;
import org.jxsd.model.Occurrence;
/** One group in a chain of groups drawn in series before the element children. */
public record GroupSpec(GroupStyle style, Occurrence occurrence) {

    public GroupSpec(Compositor groupType, Occurrence occurrence) {
        this(GroupStyle.compositor(groupType), occurrence);
    }
}
