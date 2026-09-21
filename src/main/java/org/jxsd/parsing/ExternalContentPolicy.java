package org.jxsd.parsing;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Safety policy for schema content loaded from disk or the network.
 *
 * <p>Unless {@code insecure} is set, a DOCTYPE is rejected, which removes the
 * internal-subset and external-entity vectors (XXE/SSRF). A plain-HTTP
 * dependency is always refused when credentials are configured, because the
 * Basic-auth header would travel unencrypted; an unauthenticated plain-HTTP
 * dependency is allowed with a one-off warning. {@code insecure} is the
 * explicit opt-out for trusted legacy sources and also permits plain HTTP.
 *
 * <p>Secure processing stays enabled regardless: insecure mode lifts only the
 * external-access restrictions, never the parser's expansion limits.
 */
final class ExternalContentPolicy {

    private static final byte[] DOCTYPE = "<!DOCTYPE".getBytes(StandardCharsets.US_ASCII);

    private final boolean insecure;
    private final boolean credentialsPresent;
    private final Consumer<String> onError;
    private final Consumer<String> onWarning;
    private final Set<String> warned = new HashSet<>();

    ExternalContentPolicy(boolean insecure, boolean credentialsPresent,
                          Consumer<String> onError, Consumer<String> onWarning) {
        this.insecure = insecure;
        this.credentialsPresent = credentialsPresent;
        this.onError = onError;
        this.onWarning = onWarning;
    }

    /** Whether the remote URL may be fetched under this policy. */
    boolean allowRemote(String url) {
        if (insecure || !url.startsWith("http://")) {
            return true;
        }
        if (credentialsPresent) {
            onError.accept("Refusing plain-HTTP dependency '" + url
                    + "': credentials would be sent unencrypted. Use --insecure to allow it.");
            return false;
        }
        if (warned.add(url)) {
            onWarning.accept("Plain-HTTP dependency '" + url
                    + "' is unencrypted; prefer https or use --insecure.");
        }
        return true;
    }

    /**
     * Returns {@code content} unchanged, or {@code null} after reporting an
     * error when it declares a DOCTYPE and insecure mode is off.
     */
    byte[] sanitize(byte[] content, String origin) {
        if (insecure || !containsDoctype(content)) {
            return content;
        }
        onError.accept("Refusing DOCTYPE in '" + origin
                + "': DTDs and external entities are disabled. Use --insecure to allow it.");
        return null;
    }

    private static boolean containsDoctype(byte[] content) {
        scan:
        for (int i = 0; i + DOCTYPE.length <= content.length; i++) {
            for (int j = 0; j < DOCTYPE.length; j++) {
                int symbol = content[i + j];
                if (symbol >= 'a' && symbol <= 'z') {
                    symbol -= 32;
                }
                if (symbol != DOCTYPE[j]) {
                    continue scan;
                }
            }
            return true;
        }
        return false;
    }
}
