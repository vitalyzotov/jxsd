package org.jxsd.cli;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jxsd.parsing.ComponentKind;
import org.jxsd.parsing.Schema;
import org.jxsd.parsing.SchemaComponent;

/**
 * Resolves the single diagram root from a component reference.
 *
 * <p>A reference is a local {@code NAME}, a {@code prefix:NAME} (the prefix is
 * taken from the loaded schemas or from a {@code -N} binding) or a Clark-style
 * {@code {namespace-uri}NAME}. The match must be unique; otherwise the caller
 * gets an error listing the candidates.
 */
final class RootSelector {

    private static final List<ComponentKind> KINDS = List.of(
            ComponentKind.ELEMENT, ComponentKind.COMPLEX_TYPE,
            ComponentKind.SIMPLE_TYPE, ComponentKind.GROUP);

    private RootSelector() {
    }

    static SchemaComponent select(Schema schema, String reference, String kind, Map<String, String> bindings) {
        ComponentKind expectedKind = validateKind(kind);
        Reference ref = parse(reference, schema, bindings);

        List<SchemaComponent> matches = new ArrayList<>();
        for (SchemaComponent candidate : schema.getElements()) {
            if (expectedKind != null && expectedKind != candidate.kind()) {
                continue;
            }
            if (ref.accepts(candidate) && !matches(candidate, matches)) {
                matches.add(candidate);
            }
        }

        if (matches.isEmpty()) {
            throw new IllegalArgumentException("Component '" + reference + "' not found in the schema.");
        }
        if (matches.size() > 1) {
            throw new IllegalArgumentException("Component '" + reference + "' is ambiguous ("
                    + describe(matches) + "); use -k/--kind or a namespace-qualified reference.");
        }
        return matches.getFirst();
    }

    private static boolean matches(SchemaComponent candidate, List<SchemaComponent> matches) {
        return matches.stream().anyMatch(existing -> existing.name().equals(candidate.name())
                && existing.namespace().equals(candidate.namespace())
                && existing.kind() == candidate.kind());
    }

    private static String describe(List<SchemaComponent> matches) {
        return matches.stream()
                .map(match -> "{" + match.namespace() + "}" + match.name() + " (" + match.kind().displayName() + ")")
                .collect(Collectors.joining(", "));
    }

    private static ComponentKind validateKind(String kind) {
        if (kind == null) {
            return null;
        }
        ComponentKind parsed = ComponentKind.fromDisplayName(kind);
        if (parsed == null || !KINDS.contains(parsed)) {
            String allowed = KINDS.stream().map(ComponentKind::displayName).collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                    "Unknown root kind '" + kind + "'. Use one of " + allowed + ".");
        }
        return parsed;
    }

    private static Reference parse(String reference, Schema schema, Map<String, String> bindings) {
        if (reference == null || reference.isEmpty()) {
            throw new IllegalArgumentException("The --root reference must not be empty.");
        }
        if (reference.startsWith("{")) {
            int close = reference.indexOf('}');
            if (close < 0) {
                throw new IllegalArgumentException(
                        "Malformed namespace-qualified root '" + reference + "': missing '}'.");
            }
            String namespace = reference.substring(1, close);
            String local = reference.substring(close + 1);
            if (local.isEmpty()) {
                throw new IllegalArgumentException("Malformed root '" + reference + "': missing local name.");
            }
            return new Reference(local, Set.of(namespace));
        }

        int colon = reference.indexOf(':');
        if (colon >= 0) {
            String prefix = reference.substring(0, colon);
            String local = reference.substring(colon + 1);
            if (prefix.isEmpty() || local.isEmpty()) {
                throw new IllegalArgumentException("Malformed root '" + reference + "': expected prefix:NAME.");
            }
            Set<String> namespaces = new LinkedHashSet<>();
            if (bindings.containsKey(prefix)) {
                namespaces.add(bindings.get(prefix));
            } else {
                namespaces.addAll(schema.namespaceUrisFor(prefix));
            }
            if (namespaces.isEmpty()) {
                throw new IllegalArgumentException("Unknown namespace prefix '" + prefix + "' in root '"
                        + reference + "'; bind it with -N " + prefix + "=URI.");
            }
            return new Reference(local, namespaces);
        }

        return new Reference(reference, null);
    }

    /** A local name plus an optional set of accepted namespaces ({@code null} = any). */
    private record Reference(String localName, Set<String> namespaces) {

        boolean accepts(SchemaComponent candidate) {
            return candidate.name().equals(localName)
                    && (namespaces == null || namespaces.contains(candidate.namespace()));
        }
    }
}
