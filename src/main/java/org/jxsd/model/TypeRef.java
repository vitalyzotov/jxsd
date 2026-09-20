package org.jxsd.model;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchemaType;

/** The {@code {type definition}} of an element: a named reference or an inline type. */
public sealed interface TypeRef permits TypeRef.Named, TypeRef.Anonymous {

    /** A reference to a named type definition. */
    record Named(QName name) implements TypeRef {
    }

    /** An inline anonymous type definition. */
    record Anonymous(XmlSchemaType definition) implements TypeRef {
    }
}
