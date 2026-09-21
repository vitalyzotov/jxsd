package org.jxsd.parsing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaGroup;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.utils.NamespacePrefixList;
import org.xml.sax.InputSource;

/**
 * Loads an XSD (and its includes/imports) into an Apache XmlSchema 2 object
 * model and exposes the top-level components the diagram works with.
 *
 * <p>Parsing lives in {@link SchemaReader}; dependency resolution (local files
 * and in-memory remote downloads) lives in {@link SchemaDependencyResolver}.
 * Unresolvable dependencies are tolerated: they are reported to the error
 * listener passed to {@link #load(String, Consumer)}, so the collection
 * keeps the components it could parse.
 */
public final class Schema {

    public static final String XSD_NS = "http://www.w3.org/2001/XMLSchema";

    private final List<SchemaComponent> elements = new ArrayList<>();
    private XmlSchemaCollection collection = new XmlSchemaCollection();
    private String username;
    private String password;
    private boolean insecure;

    public Schema() {
    }

    public List<SchemaComponent> getElements() { return Collections.unmodifiableList(elements); }
    public XmlSchemaCollection getCollection() { return collection; }

    /** Credentials for secured import/include URLs (the CLI's -u/-p options). */
    public void setCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /** {@code --insecure}: allow plain HTTP and DTD/external entities. */
    public void setInsecure(boolean insecure) {
        this.insecure = insecure;
    }

    /** Loads a schema and discards the reported errors. */
    public void load(String fileName) {
        load(fileName, message -> { }, message -> { });
    }

    /**
     * Loads a schema and reports errors to {@code onError}, discarding warnings.
     */
    public void load(String fileName, Consumer<String> onError) {
        load(fileName, onError, message -> { });
    }

    /**
     * Loads a schema, reporting errors to {@code onError} and policy warnings to
     * {@code onWarning}, without aborting the render. Missing files,
     * unresolvable dependencies, parse failures and content rejected by the
     * security policy are all reported this way.
     */
    public void load(String fileName, Consumer<String> onError, Consumer<String> onWarning) {
        cleanup();
        SchemaDependencyResolver resolver =
                new SchemaDependencyResolver(onError, onWarning, insecure);
        resolver.setCredentials(username, password);

        String location = fileName.trim();
        InputSource source;
        if (SchemaDependencyResolver.isRemote(location)) {
            source = resolver.remoteInput(location);
            if (source == null) {
                return;
            }
        } else {
            source = localSource(resolver, location, onError);
            if (source == null) {
                return;
            }
        }

        readSchema(source, resolver, onError);
    }

    private static InputSource localSource(SchemaDependencyResolver resolver, String location,
                                           Consumer<String> onError) {
        Path path = Paths.get(location).toAbsolutePath().normalize();
        if (!Files.exists(path)) {
            onError.accept("Schema not found: " + path);
            return null;
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException ex) {
            onError.accept("Cannot read schema '" + path + "': " + ex.getMessage());
            return null;
        }
        return resolver.sanitize(bytes, path.toUri().toString());
    }

    private void cleanup() {
        elements.clear();
        collection = new XmlSchemaCollection();
    }

    private void readSchema(InputSource source, SchemaDependencyResolver resolver, Consumer<String> onError) {
        try {
            collection = SchemaReader.read(source, resolver, elements);
        } catch (Exception ex) {
            onError.accept(ex.getMessage() == null ? ex.toString() : ex.getMessage());
            Throwable inner = ex.getCause();
            if (inner != null && inner.getMessage() != null && !inner.getMessage().equals(ex.getMessage())) {
                onError.accept(inner.getMessage());
            }
        }
    }

    /** Resolves a QName against the loaded collection, tolerating missing components. */
    public XmlSchemaType findType(QName name) {
        if (name == null) {
            return null;
        }
        return collection.getTypeByQName(name);
    }

    public XmlSchemaGroup findGroup(QName name) {
        return name == null ? null : collection.getGroupByQName(name);
    }

    /**
     * Returns the namespaces the given prefix is bound to in the loaded schemas
     * (a prefix may be re-declared by an included schema), or an empty list.
     */
    public List<String> namespaceUrisFor(String prefix) {
        List<String> uris = new ArrayList<>();
        if (prefix == null || prefix.isEmpty()) {
            return uris;
        }
        for (XmlSchema schema : collection.getXmlSchemas()) {
            NamespacePrefixList context = schema.getNamespaceContext();
            if (context == null || !declares(context, prefix)) {
                continue;
            }
            String uri = context.getNamespaceURI(prefix);
            if (uri != null && !uris.contains(uri)) {
                uris.add(uri);
            }
        }
        return uris;
    }

    private static boolean declares(NamespacePrefixList context, String prefix) {
        for (String declared : context.getDeclaredPrefixes()) {
            if (prefix.equals(declared)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a declared non-default prefix for {@code namespace}, or an empty
     * string when the namespace is not bound. Used to label inherited types as
     * the reference diagrams do ({@code ns1:Route}).
     */
    public String prefixFor(String namespace) {
        if (namespace == null || namespace.isEmpty()) {
            return "";
        }
        for (XmlSchema schema : collection.getXmlSchemas()) {
            NamespacePrefixList context = schema.getNamespaceContext();
            if (context == null) {
                continue;
            }
            for (String prefix : context.getDeclaredPrefixes()) {
                if (!prefix.isEmpty() && namespace.equals(context.getNamespaceURI(prefix))) {
                    return prefix;
                }
            }
        }
        return "";
    }
}
