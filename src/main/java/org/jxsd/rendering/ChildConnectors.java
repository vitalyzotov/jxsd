package org.jxsd.rendering;

import java.util.List;

/**
 * Draws the branch from a group's expand box to a column of child nodes. A
 * required child is reached by a solid line, an optional ({@code minOccurs = 0})
 * one by a dashed run; the vertical spine is dashed only across the optional
 * stretches. Shared by every page shape that stacks siblings.
 */
final class ChildConnectors {

    /** A child and its vertical centre, the only inputs the branch needs. */
    record Child(int minOccurs, int centerY) {
    }

    private ChildConnectors() {
    }

    /**
     * @param groupEdge   x of the group's expand box (where the branch starts)
     * @param groupCenter y of the group's centre (the branch origin)
     * @param branchX     x of the vertical spine
     * @param childX      x where the child stubs end
     */
    static void drawBranch(Connector connector, int groupEdge, int groupCenter,
                           int branchX, int childX, List<Child> children) {
        boolean anyRequired = false;
        for (Child child : children) {
            if (child.minOccurs() != 0) {
                anyRequired = true;
                break;
            }
        }
        connector.line(groupEdge, groupCenter, branchX - 1, groupCenter);
        if (!anyRequired) {
            connector.narrowMarker(branchX - 1, groupCenter);
        }
        Integer topRequired = null;
        Integer bottomRequired = null;
        for (Child child : children) {
            if (child.minOccurs() == 0) {
                continue;
            }
            int childCenter = child.centerY();
            if (childCenter < groupCenter) {
                topRequired = topRequired == null ? childCenter : Math.min(topRequired, childCenter);
            } else if (childCenter > groupCenter) {
                bottomRequired = bottomRequired == null ? childCenter : Math.max(bottomRequired, childCenter);
            }
        }
        int solidLow = topRequired == null ? groupCenter : Math.min(topRequired, groupCenter);
        int solidHigh = bottomRequired == null ? groupCenter : Math.max(bottomRequired, groupCenter);
        Integer topOptional = null;
        Integer bottomOptional = null;
        for (Child child : children) {
            if (child.minOccurs() != 0) {
                continue;
            }
            int childCenter = child.centerY();
            if (childCenter < solidLow) {
                topOptional = topOptional == null ? childCenter : Math.min(topOptional, childCenter);
            } else if (childCenter > solidHigh) {
                bottomOptional = bottomOptional == null ? childCenter : Math.max(bottomOptional, childCenter);
            }
        }
        if (topRequired != null) {
            connector.line(branchX, groupCenter, branchX, topRequired + 1);
        }
        if (topOptional != null) {
            connector.dashedVertical(branchX, solidLow, topOptional);
        }
        if (bottomRequired != null) {
            connector.line(branchX, groupCenter, branchX, bottomRequired - 1);
        }
        if (bottomOptional != null) {
            connector.dashedVertical(branchX, solidHigh, bottomOptional);
        }
        for (Child child : children) {
            int childCenter = child.centerY();
            if (child.minOccurs() == 0) {
                connector.dashedHorizontal(branchX, childCenter, childX - 1);
            } else {
                connector.line(branchX, childCenter, childX - 1, childCenter);
            }
        }
    }
}
