package jp.engawa.ses001;

import jp.engawa.ses001.dto.EmailParseResult;
import jp.engawa.ses001.service.LocalEmailProcessor;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local execution entry point.
 * Parses email file and outputs to output/ directory (gitignored).
 * <p>
 * Usage: mvn exec:java -Dexec.args="sample/mail001.el"
 * Default: sample/mail001.el
 */
public class App {

    public static void main(String[] args) {
        String inputPath = args.length > 0 ? args[0] : "sample/mail001.el";
        Path inputFile = Path.of(inputPath);

        if (!Files.exists(inputFile)) {
            System.err.println("File not found: " + inputFile.toAbsolutePath());
            System.exit(1);
        }

        Path outputDir = Path.of("output");
        try {
            LocalEmailProcessor processor = new LocalEmailProcessor(outputDir);
            try (var is = Files.newInputStream(inputFile)) {
                EmailParseResult result = processor.process(inputFile.getFileName().toString(), is);
                System.out.println("Parsed: " + outputDir.toAbsolutePath());
                System.out.println("  from: " + result.getFrom());
                System.out.println("  subject: " + result.getSubject());
                System.out.println("  json: output/json/");
                System.out.println("  body: output/body/");
                System.out.println("  attachments: output/attachments/");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
