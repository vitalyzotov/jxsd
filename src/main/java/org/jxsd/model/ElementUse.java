package org.jxsd.model;

/** How an element node was declared in the schema. */
public enum ElementUse {
    /** A top-level {@code xs:element} declaration. */
    GLOBAL,
    /** An element declared inline inside a particle. */
    LOCAL,
    /** An {@code xs:element ref} particle referring to a global declaration. */
    REFERENCE,
    /** A global element that substitutes an abstract element. */
    SUBSTITUTE
}
