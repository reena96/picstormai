# Story 0.6: Infrastructure Integration Tests

Status: done

## Story

As a QA engineer,
I want to validate infrastructure connectivity and configuration,
so that deployment is confident and reliable.

## Acceptance Criteria

1. **Database Tests**: Connection to PostgreSQL, migrations applied successfully, CRUD operations work, constraints enforced (foreign keys, unique, check), indexes exist and are used
2. **Redis Tests**: Connection test (PING/PONG), session storage (set/get), TTL (time-to-live) expiration works
3. **S3 Tests**: Upload files to S3 bucket, download files from S3, pre-signed URL generation, CORS configuration validated
4. **Health Check**: /actuator/health endpoint returns UP, all components (database, redis, s3) report healthy
5. **Testcontainers Setup**: PostgreSQL container, Redis container, LocalStack container for S3 testing
6. **Test Isolation**: Each test can run independently, tests clean up resources, no shared state between tests
7. **CI/CD Ready**: Tests run without external dependencies, all infrastructure mocked or containerized

## Tasks / Subtasks

- [ ] Task 1: Setup Testcontainers Infrastructure (AC: #5, #6, #7)
  - [ ] Add Testcontainers dependencies to build.gradle
  - [ ] Create PostgreSQL test container configuration
  - [ ] Create Redis test container configuration
  - [ ] Create LocalStack test container configuration for S3
  - [ ] Create base test class with container lifecycle management
  - [ ] Ensure containers start once and are shared across tests (@Container static)

- [ ] Task 2: Database Integration Tests (AC: #1)
  - [ ] Test PostgreSQL connection established
  - [ ] Test Flyway migrations applied (check version table)
  - [ ] Test CRUD operations on User entity
  - [ ] Test CRUD operations on UploadSession entity
  - [ ] Test CRUD operations on Photo entity
  - [ ] Test foreign key constraints (photo references session and user)
  - [ ] Test unique constraints (duplicate email)
  - [ ] Test check constraints (valid email format)
  - [ ] Verify indexes exist (user_id, session_id, tags)
  - [ ] Test query performance with indexes

- [ ] Task 3: Redis Integration Tests (AC: #2)
  - [ ] Test Redis connection (PING/PONG)
  - [ ] Test session storage (set key-value)
  - [ ] Test session retrieval (get key-value)
  - [ ] Test TTL expiration (set TTL, wait, verify expired)
  - [ ] Test session update (overwrite existing key)
  - [ ] Test session deletion (delete key)

- [ ] Task 4: S3 Integration Tests (AC: #3)
  - [ ] Test S3 bucket exists (using LocalStack)
  - [ ] Test file upload to S3
  - [ ] Test file download from S3
  - [ ] Test multipart upload for large files
  - [ ] Test pre-signed URL generation
  - [ ] Test pre-signed URL expiration
  - [ ] Test CORS configuration (allowed origins, methods)
  - [ ] Test file metadata storage

- [ ] Task 5: Health Check Implementation (AC: #4)
  - [ ] Create DatabaseHealthIndicator (extends AbstractHealthIndicator)
  - [ ] Create RedisHealthIndicator (extends AbstractHealthIndicator)
  - [ ] Create S3HealthIndicator (extends AbstractHealthIndicator)
  - [ ] Register health indicators with Spring Boot Actuator
  - [ ] Test /actuator/health endpoint returns UP
  - [ ] Test health endpoint shows status for each component
  - [ ] Test health endpoint returns DOWN when database unavailable
  - [ ] Test health endpoint returns DOWN when Redis unavailable

- [ ] Task 6: End-to-End Integration Test (AC: #1, #2, #3, #4)
  - [ ] Test complete upload flow: Client → Backend → S3 → Database
  - [ ] Test session stored in Redis during upload
  - [ ] Test photo metadata persisted to PostgreSQL
  - [ ] Test health check reports all components UP
  - [ ] Verify test runs in CI/CD without external dependencies

## Dev Notes

### Architecture Patterns and Constraints

**Testcontainers Pattern:**
- Use `@Testcontainers` annotation on test classes
- Use `@Container static` for shared containers across tests
- Containers start before all tests and stop after all tests
- Avoids container startup overhead for each test method

**LocalStack for S3:**
- LocalStack provides local AWS cloud stack
- S3 service available at http://localhost:4566
- Configure AWS SDK to use LocalStack endpoint
- No real AWS costs during testing

**Spring Boot Health Indicators:**
- Extend `AbstractHealthIndicator` for custom health checks
- Implement `doHealthCheck(Health.Builder builder)` method
- Return `UP` or `DOWN` status with details
- Automatically exposed at `/actuator/health`

**Example Health Indicator:**
```java
@Component
public class DatabaseHealthIndicator extends AbstractHealthIndicator {
    private final R2dbcEntityTemplate template;

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        template.getDatabaseClient()
            .sql("SELECT 1")
            .fetch()
            .one()
            .block(Duration.ofSeconds(5));
        builder.up().withDetail("database", "PostgreSQL");
    }
}
```

[Source: docs/epics/epic-0-foundation-infrastructure.md#Story-0.6]

### Source Tree Components to Touch

```
backend/
├── build.gradle
│   └── Add Testcontainers dependencies
├── src/main/java/com/rapidphoto/
│   ├── health/
│   │   ├── DatabaseHealthIndicator.java
│   │   ├── RedisHealthIndicator.java
│   │   └── S3HealthIndicator.java
│   └── config/
│       └── ActuatorConfig.java (if needed)
└── src/test/java/com/rapidphoto/
    ├── infrastructure/
    │   ├── BaseIntegrationTest.java (shared containers)
    │   ├── DatabaseIntegrationTest.java
    │   ├── RedisIntegrationTest.java
    │   ├── S3IntegrationTest.java
    │   ├── HealthCheckIntegrationTest.java
    │   └── EndToEndIntegrationTest.java
    └── testcontainers/
        ├── PostgresTestContainer.java
        ├── RedisTestContainer.java
        └── LocalStackTestContainer.java
```

### Testing Standards Summary

**Testcontainers Best Practices:**
- Use static containers shared across all tests in a class
- Use `@DynamicPropertySource` to inject container properties
- Clean up test data between tests (truncate tables or transactions)
- Set container resource limits (memory, CPU)

**Integration Test Patterns:**
- Use `@SpringBootTest` for full application context
- Use `@AutoConfigureWebTestClient` for WebFlux testing
- Mock external APIs, use real containers for databases
- Test realistic scenarios (not just happy path)

**Assertions:**
- Use AssertJ for fluent assertions
- Use StepVerifier for reactive streams testing
- Verify both success and failure cases
- Check error messages and status codes

### Project Structure Notes

**Alignment with Unified Project Structure:**
- Health indicators in `health/` package under main source
- Integration tests in `infrastructure/` package under test source
- Testcontainer configurations in `testcontainers/` package
- Follow existing test patterns from Story 0.3 and 0.4

**Dependencies on Previous Stories:**
- Story 0.1: AWS infrastructure setup (S3 bucket configuration)
- Story 0.2: Database schema and migrations (Flyway scripts)
- Story 0.3: Domain model (User, UploadSession, Photo entities)
- Story 0.4: CQRS handlers (for end-to-end testing)
- Story 0.5: Design system (not directly used in backend tests)

### Learnings from Previous Story

**From Story 0-4-cqrs-structure-setup (Status: done)**

- **Domain Aggregates Available**: User, UploadSession, Photo with business methods
- **Value Objects**: Email, PhotoId, S3Location for domain modeling
- **Domain Events**: UserRegisteredEvent, UploadCompletedEvent, PhotoUploadedEvent, PhotoFailedEvent
- **R2DBC Repositories**: UserRepository, UploadSessionRepository, PhotoRepository with reactive queries
- **Testing Patterns**: 87 unit tests using JUnit 5, AssertJ, StepVerifier from reactor-test
- **BCrypt Hashing**: User.create() automatically hashes passwords
- **State Machines**: SessionStatus and PhotoStatus enums enforce valid transitions

**Key Files to Use**:
- Use `backend/src/main/java/com/rapidphoto/domain/**/*.java` for domain aggregates
- Use `backend/src/main/java/com/rapidphoto/domain/**/repository/*.java` for repositories
- Follow test patterns from `backend/src/test/java/com/rapidphoto/domain/**/*Test.java`
- Integration tests should verify domain model persists correctly to PostgreSQL

**Integration Testing Approach**:
- Reuse existing domain entities and repositories
- Test that domain events are published and captured
- Verify reactive queries work with Testcontainers PostgreSQL
- Ensure constraints defined in Story 0.2 are enforced

[Source: docs/stories/0-4-cqrs-structure-setup.md#Learnings-from-Previous-Story]

### References

- Epic Overview: [Source: docs/epics/epic-0-foundation-infrastructure.md#Story-0.6]
- Testcontainers Documentation: https://www.testcontainers.org/
- LocalStack Documentation: https://docs.localstack.cloud/
- Spring Boot Actuator Health Indicators: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.health
- Story 0.2 Database Schema: [Source: docs/stories/0-2-database-schema-migrations.md]
- Story 0.3 Domain Model: [Source: docs/stories/0-3-domain-model-implementation-ddd.md]

## Dev Agent Record

### Context Reference

- docs/stories/0-6-infrastructure-integration-tests.context.xml

### Agent Model Used

claude-sonnet-4-5-20250929

### Debug Log References

### Completion Notes List

1. Implemented comprehensive infrastructure integration testing using Testcontainers
2. Created 3 health indicators for database, Redis, and S3 connectivity monitoring
3. Developed 6 integration test classes with 31 total test methods
4. All tests validate infrastructure connectivity: PostgreSQL, Redis, S3, Health endpoints
5. Added dependencies: Actuator, Redis reactive, AWS S3 SDK, Testcontainers (LocalStack, Redis)
6. Configured application.yml with Redis, S3, and Actuator settings
7. Created S3Config bean supporting both real AWS and LocalStack
8. Tests use containerized infrastructure (no external dependencies required)
9. BaseIntegrationTest provides shared container setup for all tests
10. End-to-end test validates complete upload flow across all infrastructure components

### File List

**NEW FILES:**
- backend/src/main/java/com/rapidphoto/health/DatabaseHealthIndicator.java
- backend/src/main/java/com/rapidphoto/health/RedisHealthIndicator.java
- backend/src/main/java/com/rapidphoto/health/S3HealthIndicator.java
- backend/src/main/java/com/rapidphoto/config/S3Config.java
- backend/src/test/java/com/rapidphoto/infrastructure/BaseIntegrationTest.java
- backend/src/test/java/com/rapidphoto/infrastructure/DatabaseIntegrationTest.java
- backend/src/test/java/com/rapidphoto/infrastructure/RedisIntegrationTest.java
- backend/src/test/java/com/rapidphoto/infrastructure/S3IntegrationTest.java
- backend/src/test/java/com/rapidphoto/infrastructure/HealthCheckIntegrationTest.java
- backend/src/test/java/com/rapidphoto/infrastructure/EndToEndIntegrationTest.java
- docs/validation/epic0_0.6_validation.md

**MODIFIED FILES:**
- backend/build.gradle (added Actuator, Redis, S3, LocalStack dependencies)
- backend/src/main/resources/application.yml (added Redis, S3, Actuator configuration)
