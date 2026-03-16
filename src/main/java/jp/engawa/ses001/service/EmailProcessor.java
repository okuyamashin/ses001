package jp.engawa.ses001.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import jp.engawa.ses001.dto.EmailParseResult;
import jp.engawa.ses001.parser.EmailParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Processes email from S3: parses, saves body/attachments, outputs JSON.
 */
public class EmailProcessor {

    private final AmazonS3 s3;
    private final String bucket;
    private final String outputPrefix;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmailProcessor(AmazonS3 s3, String bucket, String outputPrefix) {
        this.s3 = s3;
        this.bucket = bucket;
        this.outputPrefix = outputPrefix == null ? "" : outputPrefix.endsWith("/") ? outputPrefix : outputPrefix + "/";
    }

    public EmailParseResult process(String sourceKey, InputStream emailStream) throws Exception {
        String normalized = sourceKey.replaceAll("[^a-zA-Z0-9_-]", "_");
        final String baseId = normalized.length() > 100 ? normalized.substring(0, 100) : normalized;

        EmailParser.AttachmentHandler attachmentHandler = (filename, contentType, messageId, content) -> {
            String key = outputPrefix + "attachments/" + baseId + "/" + sanitizeFilename(filename);
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentType(contentType);
            List<Tag> tags = new ArrayList<>();
            tags.add(new Tag("filename", truncateTagValue(filename)));
            tags.add(new Tag("content-type", truncateTagValue(contentType)));
            if (messageId != null && !messageId.isEmpty()) {
                tags.add(new Tag("message-id", truncateTagValue(messageId)));
            }
            PutObjectRequest req = new PutObjectRequest(bucket, key, content, meta)
                    .withTagging(new ObjectTagging(tags));
            s3.putObject(req);
            return key;
        };

        EmailParser.BodyHandler bodyHandler = new EmailParser.BodyHandler() {
            @Override
            public String savePlain(String content, String messageId) throws IOException {
                String key = outputPrefix + "body/" + baseId + "/plain.txt";
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                ObjectMetadata meta = new ObjectMetadata();
                meta.setContentLength(bytes.length);
                meta.setContentType("text/plain; charset=UTF-8");
                List<Tag> tags = new ArrayList<>();
                tags.add(new Tag("content-type", "text/plain"));
                if (messageId != null && !messageId.isEmpty()) {
                    tags.add(new Tag("message-id", truncateTagValue(messageId)));
                }
                PutObjectRequest req = new PutObjectRequest(bucket, key, new ByteArrayInputStream(bytes), meta)
                        .withTagging(new ObjectTagging(tags));
                s3.putObject(req);
                return key;
            }

            @Override
            public String saveHtml(String content, String messageId) throws IOException {
                String key = outputPrefix + "body/" + baseId + "/body.html";
                byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                ObjectMetadata meta = new ObjectMetadata();
                meta.setContentLength(bytes.length);
                meta.setContentType("text/html; charset=UTF-8");
                List<Tag> tags = new ArrayList<>();
                tags.add(new Tag("content-type", "text/html"));
                if (messageId != null && !messageId.isEmpty()) {
                    tags.add(new Tag("message-id", truncateTagValue(messageId)));
                }
                PutObjectRequest req = new PutObjectRequest(bucket, key, new ByteArrayInputStream(bytes), meta)
                        .withTagging(new ObjectTagging(tags));
                s3.putObject(req);
                return key;
            }
        };

        EmailParser parser = new EmailParser(attachmentHandler, bodyHandler);
        EmailParseResult result = parser.parse(emailStream);

        String jsonKey = outputPrefix + "json/" + baseId + ".json";
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        ObjectMetadata meta = new ObjectMetadata();
        meta.setContentLength(jsonBytes.length);
        meta.setContentType("application/json");
        s3.putObject(bucket, jsonKey, new ByteArrayInputStream(jsonBytes), meta);

        return result;
    }

    private static String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private static String truncateTagValue(String value) {
        if (value == null) return "";
        return value.length() > 256 ? value.substring(0, 256) : value;
    }
}
