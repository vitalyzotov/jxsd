package org.jxsd.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.ws.commons.schema.XmlSchemaAnnotation;
import org.apache.ws.commons.schema.XmlSchemaAnnotationItem;
import org.apache.ws.commons.schema.XmlSchemaDocumentation;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Selects and extracts {@code xs:annotation} documentation text.
 *
 * <p>A requested {@code xml:lang} is matched case-insensitively, then the
 * language-less entry, then the first entry. A {@code null} language means "the
 * default entry" (language-less or first); an empty language means "every
 * entry", joined with a {@code (lang, source)} marker. The normalized form
 * collapses whitespace for display; the raw form keeps it verbatim.
 */
public final class AnnotationText {

    private static final Pattern SPACE_RUN = Pattern.compile("[ \\n\\t]+");

    private AnnotationText() {}

    /** Extracts display-ready documentation for the selected language. */
    public static String documentation(XmlSchemaAnnotation annotation, String language) {
        return extract(annotation, language, true);
    }

    /** Extracts documentation verbatim, preserving whitespace and newlines. */
    public static String rawDocumentation(XmlSchemaAnnotation annotation, String language) {
        return extract(annotation, language, false);
    }

    private static String extract(XmlSchemaAnnotation annotation, String language, boolean normalized) {
        List<XmlSchemaDocumentation> entries = entriesOf(annotation);
        if (entries.isEmpty()) {
            return "";
        }
        if (language != null && language.isEmpty()) {
            return concatenate(entries, normalized);
        }
        return text(select(entries, language), normalized);
    }

    private static List<XmlSchemaDocumentation> entriesOf(XmlSchemaAnnotation annotation) {
        List<XmlSchemaDocumentation> entries = new ArrayList<>();
        if (annotation == null || annotation.getItems() == null) {
            return entries;
        }
        for (XmlSchemaAnnotationItem item : annotation.getItems()) {
            if (item instanceof XmlSchemaDocumentation documentation) {
                entries.add(documentation);
            }
        }
        return entries;
    }

    private static XmlSchemaDocumentation select(List<XmlSchemaDocumentation> entries, String language) {
        if (language != null && !language.isEmpty()) {
            for (XmlSchemaDocumentation entry : entries) {
                if (language.equalsIgnoreCase(langOf(entry))) {
                    return entry;
                }
            }
        }
        for (XmlSchemaDocumentation entry : entries) {
            if (langOf(entry).isEmpty()) {
                return entry;
            }
        }
        return entries.get(0);
    }

    private static String concatenate(List<XmlSchemaDocumentation> entries, boolean normalized) {
        StringBuilder joined = new StringBuilder();
        for (XmlSchemaDocumentation entry : entries) {
            if (!joined.isEmpty()) {
                joined.append("\r\n");
            }
            joined.append(text(entry, normalized));
            String marker = marker(entry);
            if (!marker.isEmpty()) {
                joined.append(" (").append(marker).append(')');
            }
        }
        return joined.toString();
    }

    private static String text(XmlSchemaDocumentation entry, boolean normalized) {
        NodeList markup = entry.getMarkup();
        if (markup != null && markup.getLength() > 0) {
            String raw = markupText(markup);
            if (!normalized || !raw.isEmpty()) {
                return normalized ? normalize(raw) : raw;
            }
        }
        return sourceOf(entry);
    }

    private static String markupText(NodeList markup) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < markup.getLength(); i++) {
            Node node = markup.item(i);
            text.append(node.getTextContent());
        }
        return text.toString();
    }

    private static String normalize(String text) {
        return SPACE_RUN.matcher(text.replace("\r", "")).replaceAll(" ").trim();
    }

    private static String sourceOf(XmlSchemaDocumentation entry) {
        return entry.getSource() == null ? "" : entry.getSource();
    }

    private static String langOf(XmlSchemaDocumentation entry) {
        return entry.getLanguage() == null ? "" : entry.getLanguage();
    }

    private static String marker(XmlSchemaDocumentation entry) {
        String language = langOf(entry);
        String source = sourceOf(entry);
        if (!language.isEmpty() && !source.isEmpty()) {
            return language + ", " + source;
        }
        return language.isEmpty() ? source : language;
    }
}
