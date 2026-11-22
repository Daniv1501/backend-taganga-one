package com.taganga.one.assistant;

import org.springframework.web.util.HtmlUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte el Markdown ligero de Gemini en HTML seguro para el chat.
 * Primero se escapa TODO el texto (la IA nunca puede inyectar etiquetas ni scripts);
 * después se reconstruyen solo párrafos, listas, negritas y cursivas.
 */
public final class AnswerFormatter {

    private static final Pattern LIST_ITEM = Pattern.compile("^(?:[-*•]|\\d+[.)])\\s+(.*)$");
    private static final Pattern HEADING = Pattern.compile("^#{1,6}\\s+(.*)$");
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*|__(.+?)__");
    private static final Pattern ITALIC = Pattern.compile("(?<![*\\w])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![*\\w])");

    private AnswerFormatter() {
    }

    public static String toHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        // Con UTF-8 solo se escapan < > & " ' (las tildes y la ñ quedan legibles)
        String escaped = HtmlUtils.htmlEscape(markdown.replace("\r\n", "\n"), "UTF-8");
        StringBuilder html = new StringBuilder();
        StringBuilder paragraph = new StringBuilder();
        boolean inList = false;

        for (String raw : escaped.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                flushParagraph(html, paragraph);
                inList = closeList(html, inList);
                continue;
            }
            Matcher item = LIST_ITEM.matcher(line);
            if (item.matches()) {
                flushParagraph(html, paragraph);
                if (!inList) {
                    html.append("<ul>");
                    inList = true;
                }
                html.append("<li>").append(inline(item.group(1))).append("</li>");
                continue;
            }
            inList = closeList(html, inList);
            Matcher heading = HEADING.matcher(line);
            String text = heading.matches() ? "<b>" + inline(heading.group(1)) + "</b>" : inline(line);
            if (!paragraph.isEmpty()) {
                paragraph.append("<br>");
            }
            paragraph.append(text);
        }
        flushParagraph(html, paragraph);
        closeList(html, inList);
        return html.toString();
    }

    private static String inline(String text) {
        String bold = BOLD.matcher(text).replaceAll(match ->
                "<b>" + Matcher.quoteReplacement(match.group(1) != null ? match.group(1) : match.group(2)) + "</b>");
        return ITALIC.matcher(bold).replaceAll(match -> "<i>" + Matcher.quoteReplacement(match.group(1)) + "</i>");
    }

    private static void flushParagraph(StringBuilder html, StringBuilder paragraph) {
        if (!paragraph.isEmpty()) {
            html.append("<p>").append(paragraph).append("</p>");
            paragraph.setLength(0);
        }
    }

    private static boolean closeList(StringBuilder html, boolean inList) {
        if (inList) {
            html.append("</ul>");
        }
        return false;
    }
}
