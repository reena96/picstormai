# RapidPhoto Backend

Spring Boot WebFlux reactive backend for RapidPhotoUpload application.

## Technology Stack

- **Framework**: Spring Boot 3.2.0 with WebFlux (Reactive)
- **Language**: Java 17
- **Database**: PostgreSQL 15.x with R2DBC (Reactive driver)
- **Migrations**: Flyway
- **Build Tool**: Gradle
- **Testing**: JUnit 5, Testcontainers

## Project Structure

```
backend/
├── src/main/
│   ├── java/com/rapidphoto/          # Java source code
│   │   └── RapidPhotoApplication.java # Main application class
│   └── resources/
│       ├── application.yml            # Configuration
│       └── db/migration/              # Flyway migrations
│           ├── V1__create_users_tables.sql
│           ├── V2__create_upload_tables.sql
│           ├── V3__create_tagging_tables.sql
│           └── V4__add_performance_indexes.sql
├── src/test/
│   └── java/com/rapidphoto/migration/
│       └── FlywayMigrationTest.java   # Migration integration tests
├── build.gradle                       # Gradle build configuration
└── settings.gradle                    # Gradle settings
```

## Database Schema

### V1: User Tables
- **users**: Core user authentication (email, password_hash, display_name)
- **user_preferences**: User settings (animations, sound, theme, concurrent_uploads)
- **refresh_tokens**: JWT refresh tokens for session management

### V2: Upload Domain Tables
- **upload_sessions**: Batch upload session tracking
- **photos**: Photo metadata with S3 references and upload status

### V3: Tagging Tables
- **tags**: User-defined tags for photo organization
- **photo_tags**: Many-to-many relationship (photos ↔ tags)

### V4: Performance Indexes
- Gallery query optimization (user_id + created_at DESC)
- Upload session monitoring indexes
- Failed upload tracking
- Tag search optimization
- JSONB metadata queries (camera model, dimensions)

## Running Locally

### Prerequisites
- Java 17 or later
- Docker (for PostgreSQL or Testcontainers)

### Start PostgreSQL (Docker)
```bash
docker run -d \
  --name rapidphoto-db \
  -e POSTGRES_DB=rapidphoto \
  -e POSTGRES_USER=rapidphoto_admin \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  postgres:15-alpine
```

### Build and Run
```bash
./gradlew bootRun
```

### Run Tests
```bash
./gradlew test
```

The tests use Testcontainers to automatically spin up a PostgreSQL instance for integration testing.

## Configuration

Environment variables for database connection:

- `DB_HOST`: PostgreSQL host (default: localhost)
- `DB_PORT`: PostgreSQL port (default: 5432)
- `DB_NAME`: Database name (default: rapidphoto)
- `DB_USERNAME`: Database user (default: rapidphoto_admin)
- `DB_PASSWORD`: Database password (default: password)

## Flyway Migrations

Migrations run automatically on application startup. To disable:

```yaml
spring:
  flyway:
    enabled: false
```

### Migration Commands

```bash
# Migrate to latest version
./gradlew flywayMigrate

# Show migration info
./gradlew flywayInfo

# Validate migrations
./gradlew flywayValidate

# Repair migration history
./gradlew flywayRepair
```

## Testing

All tests use Testcontainers for isolated database testing:

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests FlywayMigrationTest

# Run with verbose output
./gradlew test --info
```

## Next Steps

After Story 0.2 completion:
- **Story 0.3**: Implement domain model (DDD aggregates)
- **Story 0.4**: Implement CQRS structure (commands and queries)
