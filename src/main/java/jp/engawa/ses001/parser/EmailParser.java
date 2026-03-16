package jp.engawa.ses001.parser;

import jp.engawa.ses001.dto.EmailParseResult;
import jp.engawa.ses001.util.ForwardedSenderExtractor;
import jp.engawa.ses001.util.HtmlToText;
import jp.engawa.ses001.util.UrlExtractor;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.codec.Base64InputStream;
import org.apache.james.mime4j.codec.QuotedPrintableInputStream;
import org.apache.james.mime4j.parser.AbstractContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses email using Mime4j stream parser.
 * Builds EmailParseResult with headers, forwarded sender, links, and attachment info.
 */
public class EmailParser {

    private static final Pattern EMAIL_IN_ANGLE = Pattern.compile("<([^>]+@[^>]+)>");
    private static final Pattern PLAIN_EMAIL = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");

    /**
     * Callback for handling attachment content (non-inline). Caller streams to S3.
     *
     * @return S3 key where the attachment was saved, or null
     */
    public interface AttachmentHandler {
        String handleAttachment(String filename, String contentType, String messageId, InputStream content) throws IOException;
    }

    /**
     * Callback for saving body content. Caller saves to S3 and returns the S3 key.
     */
    public interface BodyHandler {
        String savePlain(String content, String messageId) throws IOException;

        String saveHtml(String content, String messageId) throws IOException;
    }

    private final AttachmentHandler attachmentHandler;
    private final BodyHandler bodyHandler;

    public EmailParser(AttachmentHandler attachmentHandler, BodyHandler bodyHandler) {
        this.attachmentHandler = attachmentHandler;
        this.bodyHandler = bodyHandler;
    }

    public EmailParseResult parse(InputStream input) throws MimeException, IOException {
        EmailParseResult result = new EmailParseResult();
        Handler handler = new Handler(result);
        MimeStreamParser parser = new MimeStreamParser();
        parser.setContentHandler(handler);
        parser.parse(input);
        return result;
    }

    private class Handler extends AbstractContentHandler {
        private final EmailParseResult result;
        private int partDepth;
        private StringBuilder currentHeaders = new StringBuilder();
        private String contentDisposition;
        private String contentType;
        private String filename;
        private boolean isTopLevel = true;
        private final StringBuilder allBodyText = new StringBuilder();
        private String plainContent;
        private String htmlContent;

        Handler(EmailParseResult result) {
            this.result = result;
        }

        @Override
        public void startMessage() throws MimeException {
            partDepth = 0;
            isTopLevel = true;
        }

        @Override
        public void startHeader() throws MimeException {
            currentHeaders.setLength(0);
            contentDisposition = null;
            contentType = null;
            filename = null;
        }

        @Override
        public void field(Field rawField) throws MimeException {
            String name = rawField.getName();
            String body = rawField.getBody();
            if (body == null) body = "";

            currentHeaders.append(name).append(": ").append(body).append("\n");

            if (isTopLevel && partDepth == 0) {
                switch (name.toLowerCase()) {
                    case "from" -> result.setFrom(extractEmail(body));
                    case "to" -> result.setTo(parseAddressList(body));
                    case "subject" -> result.setSubject(body.trim());
                    case "date" -> result.setDate(body.trim());
                    case "message-id" -> result.setMessageId(body.trim());
                    default -> {}
                }
            }

            if ("content-disposition".equalsIgnoreCase(name)) {
                contentDisposition = body.trim().toLowerCase();
                if (body.contains("filename=")) {
                    Matcher m = Pattern.compile("filename\\s*=\\s*[\"']?([^\"';\\n]+)").matcher(body);
                    if (m.find()) {
                        filename = m.group(1).trim();
                    }
                }
            }
            if ("content-type".equalsIgnoreCase(name)) {
                contentType = body.split(";")[0].trim().toLowerCase();
            }
        }

        @Override
        public void endHeader() throws MimeException {
            // Headers collected for current part
        }

        @Override
        public void startBodyPart() throws MimeException {
            partDepth++;
            isTopLevel = false;
        }

        @Override
        public void endBodyPart() throws MimeException {
            partDepth--;
        }

        @Override
        public void body(BodyDescriptor bd, InputStream is) throws MimeException, IOException {
            String mimeType = bd.getMimeType() != null ? bd.getMimeType().toLowerCase() : "text/plain";
            boolean isInlineAttachment = "inline".equals(contentDisposition);

            InputStream decoded = wrapDecoding(is, bd.getTransferEncoding());

            if (mimeType.startsWith("text/")) {
                String content = readToString(decoded);
                allBodyText.append(mimeType.contains("html") ? stripHtml(content) : content).append("\n");

                String messageId = result.getMessageId();
                if (mimeType.contains("html")) {
                    htmlContent = (htmlContent == null) ? content : htmlContent;
                    String s3Key = bodyHandler != null ? bodyHandler.saveHtml(content, messageId) : null;
                    if (result.getBody() == null) result.setBody(new EmailParseResult.BodyPaths());
                    result.getBody().setHtml(s3Key);
                } else {
                    plainContent = (plainContent == null) ? content : plainContent;
                    String s3Key = bodyHandler != null ? bodyHandler.savePlain(content, messageId) : null;
                    if (result.getBody() == null) result.setBody(new EmailParseResult.BodyPaths());
                    result.getBody().setPlain(s3Key);
                }
            } else {
                if (!isInlineAttachment && attachmentHandler != null) {
                    String fn = filename != null ? filename : "attachment_" + System.currentTimeMillis();
                    String messageId = result.getMessageId();
                    String s3Key = attachmentHandler.handleAttachment(fn, mimeType, messageId, decoded);
                    if (s3Key != null) {
                        result.getAttachments().add(new EmailParseResult.AttachmentInfo(s3Key, fn, mimeType));
                    }
                }
            }
        }

        @Override
        public void endMessage() throws MimeException {
            String bodyText = allBodyText.toString();
            result.setForwardedFrom(ForwardedSenderExtractor.extract(bodyText));
            result.setLinks(UrlExtractor.extract(bodyText));

            String excerpt;
            if (plainContent != null && !plainContent.isEmpty()) {
                excerpt = HtmlToText.truncate(plainContent, HtmlToText.getExcerptLength());
            } else if (htmlContent != null && !htmlContent.isEmpty()) {
                excerpt = HtmlToText.truncate(HtmlToText.convert(htmlContent), HtmlToText.getExcerptLength());
            } else {
                excerpt = null;
            }
            result.setExcerpt(excerpt);
        }

        private String stripHtml(String html) {
            return html.replaceAll("<[^>]+>", " ").replaceAll("&nbsp;", " ").replaceAll("\\s+", " ");
        }

        private InputStream wrapDecoding(InputStream is, String encoding) {
            if (encoding == null) return is;
            return switch (encoding.toLowerCase()) {
                case "base64" -> new Base64InputStream(is);
                case "quoted-printable" -> new QuotedPrintableInputStream(is);
                default -> is;
            };
        }

        private String readToString(InputStream is) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) {
                baos.write(buf, 0, n);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }

        private String extractEmail(String value) {
            if (value == null) return null;
            Matcher angle = EMAIL_IN_ANGLE.matcher(value);
            if (angle.find()) return angle.group(1).trim();
            Matcher plain = PLAIN_EMAIL.matcher(value);
            if (plain.find()) return plain.group();
            return value.trim();
        }

        private List<String> parseAddressList(String value) {
            List<String> list = new ArrayList<>();
            if (value == null) return list;
            Matcher angle = EMAIL_IN_ANGLE.matcher(value);
            while (angle.find()) {
                list.add(angle.group(1).trim());
            }
            if (list.isEmpty()) {
                Matcher plain = PLAIN_EMAIL.matcher(value);
                while (plain.find()) {
                    list.add(plain.group());
                }
            }
            return list;
        }
    }
}
