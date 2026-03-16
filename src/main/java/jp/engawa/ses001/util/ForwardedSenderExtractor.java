package jp.engawa.ses001.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the original sender from forwarded email content.
 * <p>
 * Handles common forwarded message formats:
 * - "From: user@example.com"
 * - "From: Name &lt;user@example.com&gt;"
 * - "-----Original Message-----" / "----- 転送メッセージ -----" followed by From:
 */
public final class ForwardedSenderExtractor {

    // From: email or From: Name <email>
    private static final Pattern FROM_LINE = Pattern.compile(
            "From:\\s*(.+?)(?:\\r?\\n|$)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    // Extract email from "Name <email@example.com>" or plain "email@example.com"
    private static final Pattern EMAIL_IN_ANGLE = Pattern.compile("<([^>]+@[^>]+)>");
    private static final Pattern PLAIN_EMAIL = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    // Forwarded message separators (content after these is the original message)
    private static final Pattern FORWARD_SEPARATOR = Pattern.compile(
            "-----\\s*(?:Original Message|転送メッセージ|Forwarded message|Fwd:)",
            Pattern.CASE_INSENSITIVE
    );

    private ForwardedSenderExtractor() {}

    /**
     * Extracts the original sender email from forwarded content.
     * Looks for "From:" lines that typically appear in forwarded message blocks.
     *
     * @param bodyContent full body text (plain or HTML-stripped)
     * @return the original sender email, or null if not found
     */
    public static String extract(String bodyContent) {
        if (bodyContent == null || bodyContent.isEmpty()) {
            return null;
        }

        // Prefer content after forward separator
        String searchArea = bodyContent;
        Matcher sepMatcher = FORWARD_SEPARATOR.matcher(bodyContent);
        if (sepMatcher.find()) {
            searchArea = bodyContent.substring(sepMatcher.end());
        }

        Matcher fromMatcher = FROM_LINE.matcher(searchArea);
        while (fromMatcher.find()) {
            String fromValue = fromMatcher.group(1).trim();
            String email = extractEmail(fromValue);
            if (email != null && !email.isEmpty()) {
                return email;
            }
        }

        return null;
    }

    private static String extractEmail(String fromValue) {
        Matcher angle = EMAIL_IN_ANGLE.matcher(fromValue);
        if (angle.find()) {
            return angle.group(1).trim();
        }
        Matcher plain = PLAIN_EMAIL.matcher(fromValue);
        if (plain.find()) {
            return plain.group();
        }
        return null;
    }
}
