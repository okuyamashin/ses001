package jp.engawa.ses001.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jp.engawa.ses001.dto.EmailParseResult;
import jp.engawa.ses001.parser.EmailParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Processes email from local file: parses, saves body/attachments to filesystem.
 * Output directory is gitignored (e.g., output/).
 */
public class LocalEmailProcessor {

    private final Path outputDir;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LocalEmailProcessor(Path outputDir) {
        this.outputDir = outputDir;
    }

    public EmailParseResult process(String sourceName, InputStream emailStream) throws Exception {
        String baseId = sourceName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (baseId.length() > 100) {
            baseId = baseId.substring(0, 100);
        }
        final String finalBaseId = baseId;

        EmailParser.AttachmentHandler attachmentHandler = (filename, contentType, messageId, content) -> {
            Path dir = outputDir.resolve("attachments").resolve(finalBaseId);
            Files.createDirectories(dir);
            Path file = dir.resolve(sanitizeFilename(filename));
            Files.copy(content, file);
            return "attachments/" + finalBaseId + "/" + sanitizeFilename(filename);
        };

        EmailParser.BodyHandler bodyHandler = new EmailParser.BodyHandler() {
            @Override
            public String savePlain(String content, String messageId) throws IOException {
                Path dir = outputDir.resolve("body").resolve(finalBaseId);
                Files.createDirectories(dir);
                Path file = dir.resolve("plain.txt");
                Files.writeString(file, content, StandardCharsets.UTF_8);
                return "body/" + finalBaseId + "/plain.txt";
            }

            @Override
            public String saveHtml(String content, String messageId) throws IOException {
                Path dir = outputDir.resolve("body").resolve(finalBaseId);
                Files.createDirectories(dir);
                Path file = dir.resolve("body.html");
                Files.writeString(file, content, StandardCharsets.UTF_8);
                return "body/" + finalBaseId + "/body.html";
            }
        };

        EmailParser parser = new EmailParser(attachmentHandler, bodyHandler);
        EmailParseResult result = parser.parse(emailStream);

        Path jsonFile = outputDir.resolve("json").resolve(finalBaseId + ".json");
        Files.createDirectories(jsonFile.getParent());
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        Files.writeString(jsonFile, json, StandardCharsets.UTF_8);

        return result;
    }

    private static String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
