# Architecture

## System Overview

```
[SES] → [S3 Bucket] → [Lambda] → [JSON / Attachment Output]
                ↓
          [Move to processed folder]
```

## Data Flow

1. **Ingestion**: Emails are stored in S3 bucket (e.g., `inbox/`)
2. **Trigger**: Lambda is invoked by S3 object created event
3. **Processing**:
   - Fetch email file (.eml) from S3
   - Parse with Mime4j
   - Output parsed result as JSON
   - Extract and save attachments
4. **Post-processing**: Move original email to `processed/` (copy + delete)

## S3 Bucket Structure (Proposed)

```
bucket/
├── inbox/           # Incoming emails (Lambda trigger)
├── processed/       # Processed emails (archive)
├── output/
│   ├── json/        # Parsed result JSON
│   └── attachments/ # Attachments
```

## Permissions

Grant the following to Lambda's IAM role:

- S3: Read, write, delete on target bucket
- CloudWatch Logs: Log output

Bucket names and paths are obtained at runtime from events and environment variables, not hardcoded in source.
