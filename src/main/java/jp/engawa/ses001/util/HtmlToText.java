package jp.engawa.ses001.util;

/**
 * Converts HTML to plain text for excerpt.
 * <br/>, <br>, <p> become newlines; other tags are stripped.
 */
public final class HtmlToText {

    private static final int EXCERPT_LENGTH = 2000;

    private HtmlToText() {}

    /**
     * Converts HTML to plain text.
     * Replaces &lt;br/&gt;, &lt;br&gt;, &lt;p&gt; with newlines; strips other tags.
     */
    public static String convert(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        String s = html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)<br>", "\n")
                .replaceAll("(?i)<p\\s*[^>]*>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("<[^>]+>", "")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("[ \t]+", " ")
                .replaceAll("\n[ \t]+", "\n")
                .replaceAll("[ \t]+\n", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
        return s;
    }

    /**
     * Returns first ~2000 chars of text.
     */
    public static String truncate(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen);
    }

    public static int getExcerptLength() {
        return EXCERPT_LENGTH;
    }
}
