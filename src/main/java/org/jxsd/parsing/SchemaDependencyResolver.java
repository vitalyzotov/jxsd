package org.jxsd.parsing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.ws.commons.schema.resolver.URIResolver;
import org.xml.sax.InputSource;

/**
 * Resolves XSD include/import dependencies for a {@link Schema}. Local files are
 * resolved against the including document; remote schemas are downloaded once
 * per URL into memory, with optional Basic authentication and any DOCTYPE
 * declaration removed so the parser never reaches out for a DTD. A dependency
 * that cannot be resolved yields {@code null} and is reported to the error
 * listener instead of aborting the load.
 */
final class SchemaDependencyResolver implements URIResolver {

    private static final String DOCTYPE = "<!DOCTYPE";

    private final Consumer<String> onError;
    private final Map<String, byte[]> downloads = new HashMap<>();
    private String user = "";
    private String secret = "";

    SchemaDependencyResolver(Consumer<String> onError) {
        this.onError = onError;
    }

    /** Credentials for a secured dependency URL (the CLI's {@code -u}/{@code -p}). */
    void setCredentials(String username, String password) {
        this.user = username == null ? "" : username;
        this.secret = password == null ? "" : password;
    }

    @Override
    public InputSource resolveEntity(String namespace, String schemaLocation, String baseUri) {
        String location = schemaLocation == null ? "" : schemaLocation.trim();
        if (location.isEmpty()) {
            return null;
        }
        if (isRemote(location)) {
            return remoteInput(location);
        }
        InputSource local = resolveAgainst(baseUri, location);
        if (local != null) {
            return local;
        }
        onError.accept("Dependency not found: " + location);
        return null;
    }

    private InputSource resolveAgainst(String base, String location) {
        if (base == null || base.isEmpty()) {
            return null;
        }
        try {
            URI resolved = new URI(base).resolve(location.replace('\\', '/'));
            if ("file".equals(resolved.getScheme())) {
                File target = new File(resolved);
                return target.exists() ? new InputSource(resolved.toString()) : null;
            }
            return remoteInput(resolved.toString());
        } catch (URISyntaxException | IllegalArgumentException ex) {
            return null;
        }
    }

    static boolean isRemote(String location) {
        return location.startsWith("http://") || location.startsWith("https://");
    }

    /**
     * Downloads a remote schema into memory and returns a re-readable
     * {@link InputSource} whose system id is the URL.
     */
    InputSource remoteInput(String url) {
        byte[] bytes = downloads.computeIfAbsent(url, this::fetch);
        if (bytes == null) {
            return null;
        }
        InputSource source = new InputSource(new ByteArrayInputStream(bytes));
        source.setSystemId(url);
        return source;
    }

    private byte[] fetch(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException ex) {
            onError.accept("Invalid dependency URL: " + url);
            return null;
        }
        try {
            URLConnection connection = uri.toURL().openConnection();
            applyCredentials(connection);
            byte[] data;
            try (InputStream in = connection.getInputStream()) {
                data = in.readAllBytes();
            }
            String xml = new String(data, StandardCharsets.UTF_8);
            return stripDoctype(xml).getBytes(StandardCharsets.UTF_8);
        } catch (IOException ex) {
            onError.accept("Dependency unavailable: " + url + " (" + ex + ")");
            return null;
        }
    }

    private void applyCredentials(URLConnection connection) {
        if (!(connection instanceof HttpURLConnection) || user.isEmpty()) {
            return;
        }
        String pair = user + ":" + secret;
        String token = Base64.getEncoder().encodeToString(pair.getBytes(StandardCharsets.UTF_8));
        connection.setRequestProperty("Authorization", "Basic " + token);
    }

    /** Removes every {@code <!DOCTYPE ...>} declaration from the document. */
    private static String stripDoctype(String xml) {
        StringBuilder cleaned = new StringBuilder();
        int cursor = 0;
        while (cursor < xml.length()) {
            int start = indexOfDoctype(xml, cursor);
            if (start < 0) {
                break;
            }
            int end = xml.indexOf('>', start);
            if (end < 0) {
                break;
            }
            cleaned.append(xml, cursor, start);
            cursor = end + 1;
        }
        cleaned.append(xml, cursor, xml.length());
        return cleaned.toString();
    }

    private static int indexOfDoctype(String xml, int from) {
        for (int i = from; i + DOCTYPE.length() <= xml.length(); i++) {
            if (xml.regionMatches(true, i, DOCTYPE, 0, DOCTYPE.length())) {
                return i;
            }
        }
        return -1;
    }
}
