# ses001

Email parsing system using AWS Lambda triggered by S3 events. Parses emails (.eml) stored in S3 and outputs JSON and attachments.

## Overview

- **Trigger**: Amazon S3 object created event
- **Processing**: Email parsing, JSON output, attachment extraction
- **Runtime**: AWS Lambda (Java 17)

## Specification

### Data Flow

1. Email (.eml) is stored in S3 bucket (e.g., `inbox/`)
2. S3 object-created event triggers Lambda
3. Lambda parses the email, saves body/attachments, outputs JSON
4. Original email is moved to `processed/` (copy + delete)

### Output Structure (S3)

When deployed to Lambda, output is written under `output/` prefix:

```
bucket/
├── inbox/              # Incoming emails (trigger)
├── processed/          # Processed emails (archive)
└── output/
    ├── json/          # Parsed result JSON (S3 keys for body/attachments)
    ├── body/          # Plain text and HTML body
    └── attachments/   # Attachments (excluding inline)
```

### Output Structure (Local)

When running locally with `mvn exec:java`, output goes to `output/` directory (gitignored):

```
output/
├── json/
├── body/
└── attachments/
```

### JSON Schema

The JSON contains S3 object keys (or local paths) for body and attachments:

| Field | Type | Description |
|-------|------|-------------|
| from | string | Sender email |
| to | string[] | Recipient emails |
| subject | string | Subject (may be MIME-encoded) |
| date | string | Date header |
| messageId | string | Message-ID header |
| forwardedFrom | string | Original sender when forwarded (null if not) |
| links | string[] | URLs extracted from body |
| attachments | object[] | Attachment info (s3Key, filename, contentType) |
| body | object | Body S3 keys: plain, html |
| excerpt | string | First ~2000 chars of plain text (or HTML converted to text) |

**attachments** (per item):

| Field | Type | Description |
|-------|------|--------------|
| s3Key | string | S3 object key |
| filename | string | Original filename |
| contentType | string | MIME type |

**body**:

| Field | Type | Description |
|-------|------|--------------|
| plain | string | S3 key for text/plain |
| html | string | S3 key for text/html |

### S3 Object Tags

- **Body objects** (plain.txt, body.html): `content-type`, `message-id`
- **Attachments**: `filename`, `content-type`, `message-id`

### Attachments

- **attachment** (Content-Disposition: attachment): Saved to S3, included in JSON
- **inline** (Content-Disposition: inline): Not saved

### Excerpt

- If `text/plain` exists: first 2000 characters
- If only HTML: HTML converted to text (`<br/>`, `<p>` → newlines, tags stripped), then first 2000 characters

## Requirements

- Java 17
- Maven 3.6+

## Build

```bash
mvn clean package
```

## Run (Local)

```bash
# Default: sample/mail001.el
mvn exec:java

# Specify input file
mvn exec:java -Dexec.args="sample/mail001.el"
```

Output is written to `output/` (gitignored).

## Project Structure

```
ses001/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/jp/engawa/ses001/
│   │   │   ├── App.java          # Local execution (mvn exec:java)
│   │   │   └── S3EmailHandler.java  # Lambda entry point
│   │   └── resources/
│   └── test/
│       ├── java/jp/engawa/ses001/
│       │   └── AppTest.java
│       └── resources/
└── docs/
```

## Main Dependencies

| Library | Purpose |
|---------|---------|
| aws-lambda-java-core | Lambda handler |
| aws-lambda-java-events | S3 event types |
| aws-java-sdk-s3 | S3 object operations |
| apache-mime4j-core | Email parsing |
| jackson-databind | JSON output |
| junit-jupiter | Testing |

## Lambda Deployment

1. Build and create Lambda JAR (includes all dependencies)
   ```bash
   mvn clean package
   ```

2. Upload `target/ses001-1.0-SNAPSHOT-lambda.jar` to Lambda

3. Configure Lambda handler
   ```
   jp.engawa.ses001.S3EmailHandler::handleRequest
   ```

4. Configure S3 bucket event notification to trigger Lambda

## License

TBD
