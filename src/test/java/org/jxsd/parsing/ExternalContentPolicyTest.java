package org.jxsd.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/** Safety rules for schema content loaded from disk or the network. */
class ExternalContentPolicyTest {

    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();

    private ExternalContentPolicy policy(boolean insecure, boolean credentials) {
        return new ExternalContentPolicy(insecure, credentials, errors::add, warnings::add);
    }

    @Test
    void httpsIsAlwaysAllowed() {
        assertTrue(policy(false, true).allowRemote("https://example.org/a.xsd"));
    }

    @Test
    void authenticatedPlainHttpIsRefused() {
        ExternalContentPolicy policy = policy(false, true);

        assertFalse(policy.allowRemote("http://example.org/a.xsd"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("plain-HTTP"), errors.get(0));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void anonymousPlainHttpIsAllowedWithOneWarning() {
        ExternalContentPolicy policy = policy(false, false);

        assertTrue(policy.allowRemote("http://example.org/a.xsd"));
        assertTrue(policy.allowRemote("http://example.org/a.xsd"));
        assertEquals(1, warnings.size(), warnings.toString());
        assertTrue(errors.isEmpty());
    }

    @Test
    void insecureAllowsPlainHttpAndDoctypes() {
        ExternalContentPolicy policy = policy(true, true);
        byte[] content = "<!DOCTYPE x [<!ENTITY e SYSTEM 'http://x'>]>"
                .getBytes(StandardCharsets.UTF_8);

        assertTrue(policy.allowRemote("http://example.org/a.xsd"));
        assertSame(content, policy.sanitize(content, "d"));
        assertTrue(errors.isEmpty());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void doctypeIsRefusedByDefault() {
        ExternalContentPolicy policy = policy(false, false);
        byte[] content = "  <!doctype X SYSTEM \"x\">".getBytes(StandardCharsets.UTF_8);

        assertNull(policy.sanitize(content, "d"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("DOCTYPE"), errors.get(0));
    }

    @Test
    void plainContentPassesThroughUnchanged() {
        ExternalContentPolicy policy = policy(false, false);
        byte[] content = "<schema/>".getBytes(StandardCharsets.UTF_8);

        assertSame(content, policy.sanitize(content, "d"));
        assertTrue(errors.isEmpty());
    }
}
