# ses001

Email parsing system using AWS Lambda triggered by S3 events. Parses emails (.eml) stored in S3 and outputs JSON and attachments.

## Overview

- **Trigger**: Amazon S3 object created event
- **Processing**: Email parsing, JSON output, attachment extraction
- **Runtime**: AWS Lambda (Java 17)

## Requirements

- Java 17
- Maven 3.6+

## Build

```bash
mvn clean package
```

## Run (Local)

```bash
mvn exec:java
```

## Project Structure

```
ses001/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/jp/engawa/ses001/
│   │   │   └── App.java
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

1. Build and create JAR
   ```bash
   mvn clean package
   ```

2. Upload `target/ses001-1.0-SNAPSHOT.jar` to Lambda

3. Configure Lambda handler (change according to your implementation)
   ```
   jp.engawa.ses001.YourHandler::handleRequest
   ```

4. Configure S3 bucket event notification to trigger Lambda

## License

TBD
