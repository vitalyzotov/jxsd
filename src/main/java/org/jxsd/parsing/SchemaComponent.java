package org.jxsd.parsing;

import java.util.Objects;

import org.apache.ws.commons.schema.XmlSchemaObject;

/** A top-level schema component registered by {@link Schema}. */
public record SchemaComponent(String name, String namespace, ComponentKind kind, XmlSchemaObject tag) {

    public SchemaComponent {
        namespace = Objects.requireNonNullElse(namespace, "");
    }
}
