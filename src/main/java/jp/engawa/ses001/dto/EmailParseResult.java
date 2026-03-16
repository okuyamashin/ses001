package jp.engawa.ses001.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of email parsing for JSON output.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmailParseResult {

    private String from;
    private List<String> to = new ArrayList<>();
    private String subject;
    private String date;
    private String messageId;
    /** Sender of the original message when this is a forwarded email */
    private String forwardedFrom;
    /** URLs extracted from body content */
    private List<String> links = new ArrayList<>();
    /** Attachments (excluding inline): s3Key, filename, contentType */
    private List<AttachmentInfo> attachments = new ArrayList<>();
    /** Body S3 keys */
    private BodyPaths body;
    /** First ~2000 chars of plain text (from text/plain, or HTML converted to text) */
    private String excerpt;

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to != null ? to : new ArrayList<>();
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getForwardedFrom() {
        return forwardedFrom;
    }

    public void setForwardedFrom(String forwardedFrom) {
        this.forwardedFrom = forwardedFrom;
    }

    public List<String> getLinks() {
        return links;
    }

    public void setLinks(List<String> links) {
        this.links = links != null ? links : new ArrayList<>();
    }

    public List<AttachmentInfo> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentInfo> attachments) {
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }

    public BodyPaths getBody() {
        return body;
    }

    public void setBody(BodyPaths body) {
        this.body = body;
    }

    public String getExcerpt() {
        return excerpt;
    }

    public void setExcerpt(String excerpt) {
        this.excerpt = excerpt;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AttachmentInfo {
        private String s3Key;
        private String filename;
        private String contentType;

        public AttachmentInfo() {}

        public AttachmentInfo(String s3Key, String filename, String contentType) {
            this.s3Key = s3Key;
            this.filename = filename;
            this.contentType = contentType;
        }

        public String getS3Key() {
            return s3Key;
        }

        public void setS3Key(String s3Key) {
            this.s3Key = s3Key;
        }

        public String getFilename() {
            return filename;
        }

        public void setFilename(String filename) {
            this.filename = filename;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BodyPaths {
        /** S3 key for plain text body */
        private String plain;
        /** S3 key for HTML body */
        private String html;

        public String getPlain() {
            return plain;
        }

        public void setPlain(String plain) {
            this.plain = plain;
        }

        public String getHtml() {
            return html;
        }

        public void setHtml(String html) {
            this.html = html;
        }
    }
}
