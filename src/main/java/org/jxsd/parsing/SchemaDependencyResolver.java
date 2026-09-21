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
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.ws.commons.schema.resolver.URIResolver;
import org.xml.sax.InputSource;

/**
 * Resolves XSD include/import dependencies for a {@link Schema}. Local files are
 * resolved against the including document; remote schemas are downloaded once
 * per URL into memory, with optional Basic authentication. Every input passes
 * through an {@link ExternalContentPolicy}: DOCTYPEs are refused and
 * unauthenticated plain HTTP is warned about unless the CLI set
 * {@code --insecure}. A dependency that cannot be resolved yields {@code null}
 * and is reported to the error listener instead of aborting the load.
 */
final class SchemaDependencyResolver implements URIResolver {

    private final Consumer<String> onError;
    private final Consumer<String> onWarning;
    private final Map<String, byte[]> downloads = new HashMap<>();
    private final boolean insecure;
    private ExternalContentPolicy policy;
    private String user = "";
    private String secret = "";

    SchemaDependencyResolver(Consumer<String> onError, Consumer<String> onWarning, boolean insecure) {
        this.onError = onError;
        this.onWarning = onWarning;
        this.insecure = insecure;
        this.policy = new ExternalContentPolicy(insecure, false, onError, onWarning);
    }

    /** Credentials for a secured dependency URL (the CLI's {@code -u}/{@code -p}). */
    void setCredentials(String username, String password) {
        this.user = username == null ? "" : username;
        this.secret = password == null ? "" : password;
        this.policy = new ExternalContentPolicy(insecure, !user.isEmpty(), onError, onWarning);
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
                if (!target.exists()) {
                    return null;
                }
                return localInput(resolved, target);
            }
            return remoteInput(resolved.toString());
        } catch (URISyntaxException | IllegalArgumentException ex) {
            return null;
        }
    }

    private InputSource localInput(URI resolved, File target) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(target.toPath());
        } catch (IOException ex) {
            onError.accept("Cannot read dependency '" + resolved + "': " + ex.getMessage());
            return null;
        }
        return sanitized(bytes, resolved.toString());
    }

    static boolean isRemote(String location) {
        return location.startsWith("http://") || location.startsWith("https://");
    }

    /**
     * Downloads a remote schema into memory and returns a re-readable
     * {@link InputSource} whose system id is the URL.
     */
    InputSource remoteInput(String url) {
        if (!policy.allowRemote(url)) {
            return null;
        }
        byte[] bytes = downloads.computeIfAbsent(url, this::fetch);
        if (bytes == null) {
            return null;
        }
        return sanitized(bytes, url);
    }

    private InputSource sanitized(byte[] bytes, String origin) {
        byte[] safe = policy.sanitize(bytes, origin);
        if (safe == null) {
            return null;
        }
        InputSource source = new InputSource(new ByteArrayInputStream(safe));
        source.setSystemId(origin);
        return source;
    }

    /** Applies the content policy to a caller-supplied document. */
    InputSource sanitize(byte[] bytes, String origin) {
        return sanitized(bytes, origin);
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
            try (InputStream in = connection.getInputStream()) {
                return in.readAllBytes();
            }
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
}
