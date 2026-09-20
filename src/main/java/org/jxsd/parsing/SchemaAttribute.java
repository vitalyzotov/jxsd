package org.jxsd.parsing;

import org.apache.ws.commons.schema.XmlSchemaAttribute;

/** A displayed attribute together with the Apache XmlSchema component it came from. */
public final class SchemaAttribute {

    private final String name;
    private final XmlSchemaAttribute tag;

    public SchemaAttribute(String name, XmlSchemaAttribute tag) {
        this.name = name;
        this.tag = tag;
    }

    public String name() { return name; }

    public XmlSchemaAttribute tag() { return tag; }
}
