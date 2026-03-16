package jp.engawa.ses001.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts URLs from text content.
 */
public final class UrlExtractor {

    // Matches http, https, and common URL patterns
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s<>\"')\\]\\]]+",
            Pattern.CASE_INSENSITIVE
    );

    private UrlExtractor() {}

    /**
     * Extracts all URLs from the given text.
     *
     * @param text text content (plain or HTML)
     * @return list of unique URLs found
     */
    public static List<String> extract(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<String> urls = new ArrayList<>();
        Matcher m = URL_PATTERN.matcher(text);
        while (m.find()) {
            String url = m.group().replaceAll("[.,;:!?]+$", "");
            if (!urls.contains(url)) {
                urls.add(url);
            }
        }
        return urls;
    }
}
