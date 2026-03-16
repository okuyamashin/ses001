package jp.engawa.ses001;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.lambda.runtime.events.models.s3.S3EventNotification;
import jp.engawa.ses001.dto.EmailParseResult;
import jp.engawa.ses001.service.EmailProcessor;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Lambda entry point for S3-triggered email parsing.
 * <p>
 * Handler: jp.engawa.ses001.S3EmailHandler::handleRequest
 */
public class S3EmailHandler implements RequestHandler<S3Event, String> {

    private final AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();

    @Override
    public String handleRequest(S3Event input, Context context) {
        if (input == null || input.getRecords() == null || input.getRecords().isEmpty()) {
            return "No records";
        }

        List<S3EventNotification.S3EventNotificationRecord> records = input.getRecords();
        for (S3EventNotification.S3EventNotificationRecord record : records) {
            String bucket = record.getS3().getBucket().getName();
            String key = decodeKey(record.getS3().getObject().getKey());
            context.getLogger().log("Processing: s3://" + bucket + "/" + key);

            try {
                EmailProcessor processor = new EmailProcessor(s3, bucket, "output/");
                try (S3Object s3Object = s3.getObject(bucket, key);
                     InputStream is = s3Object.getObjectContent()) {
                    EmailParseResult result = processor.process(key, is);
                    context.getLogger().log("Parsed: from=" + result.getFrom() + ", links=" + result.getLinks().size());
                }

                moveToProcessed(bucket, key, context);
            } catch (Exception e) {
                context.getLogger().log("Error: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }

        return "Processed " + records.size() + " record(s)";
    }

    private String decodeKey(String key) {
        if (key == null) return null;
        try {
            return URLDecoder.decode(key, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return key;
        }
    }

    private void moveToProcessed(String bucket, String key, Context context) {
        try {
            String processedKey = "processed/" + key;
            s3.copyObject(bucket, key, bucket, processedKey);
            s3.deleteObject(bucket, key);
            context.getLogger().log("Moved to: " + processedKey);
        } catch (Exception e) {
            context.getLogger().log("Warning: failed to move to processed: " + e.getMessage());
        }
    }
}
