# Story 0.4: CQRS Structure Setup

Status: drafted

## Story

As a backend developer,
I want to implement CQRS pattern with command/query handlers,
so that writes and reads are separated for clarity and scalability.

## Acceptance Criteria

1. **Commands Created**: RegisterUserCommand, LoginCommand, VerifyEmailCommand, StartUploadSessionCommand, InitiatePhotoUploadCommand, CompletePhotoUploadCommand, AddTagToPhotoCommand, RemoveTagFromPhotoCommand
2. **Command Handlers Created**: Transactional command handlers that mutate state and return IDs
3. **Queries Created**: GetUserByIdQuery, GetUserPreferencesQuery, GetUploadSessionQuery, GetActiveSessionsForUserQuery, GetPhotosForUserQuery, GetPhotosByTagQuery, GetPhotoDetailsQuery
4. **Query Handlers Created**: Read-only query handlers that return DTOs (never domain entities)
5. **DTOs Created**: UserDTO, UserPreferencesDTO, UploadSessionDTO, PhotoDTO for read models
6. **Command Handlers are Transactional**: Use @Transactional annotation, mutate state, return IDs
7. **Query Handlers Return DTOs**: Read-only operations, return DTOs to prevent domain object mutation
8. **Domain Events Published**: Command handlers publish domain events using ApplicationEventPublisher
9. **Unit Tests**: Unit tests for all command and query handlers
10. **Integration Tests**: Integration test executing command and verifying with query

## Tasks / Subtasks

- [ ] Task 1: Create Command Objects (AC: #1)
  - [ ] Create RegisterUserCommand (email, password, displayName)
  - [ ] Create LoginCommand (email, password)
  - [ ] Create VerifyEmailCommand (userId)
  - [ ] Create StartUploadSessionCommand (userId, totalPhotos)
  - [ ] Create InitiatePhotoUploadCommand (userId, sessionId, filename, fileSize)
  - [ ] Create CompletePhotoUploadCommand (photoId, metadata)
  - [ ] Create AddTagToPhotoCommand (photoId, tagId)
  - [ ] Create RemoveTagFromPhotoCommand (photoId, tagId)

- [ ] Task 2: Create Command Handlers (AC: #2, #6, #8)
  - [ ] Create RegisterUserCommandHandler - creates User, publishes UserRegisteredEvent, returns userId
  - [ ] Create LoginCommandHandler - validates credentials, records login, returns success/failure
  - [ ] Create VerifyEmailCommandHandler - calls User.verifyEmail(), saves
  - [ ] Create StartUploadSessionCommandHandler - creates UploadSession, returns sessionId
  - [ ] Create InitiatePhotoUploadCommandHandler - creates Photo, returns photoId
  - [ ] Create CompletePhotoUploadCommandHandler - completes Photo, publishes PhotoUploadedEvent
  - [ ] Create AddTagToPhotoCommandHandler - adds tag to Photo
  - [ ] Create RemoveTagFromPhotoCommandHandler - removes tag from Photo
  - [ ] Add @Transactional(rollbackFor = Exception.class) to all handlers
  - [ ] Inject ApplicationEventPublisher to publish domain events

- [ ] Task 3: Create Query Objects (AC: #3)
  - [ ] Create GetUserByIdQuery (userId)
  - [ ] Create GetUserPreferencesQuery (userId)
  - [ ] Create GetUploadSessionQuery (sessionId)
  - [ ] Create GetActiveSessionsForUserQuery (userId)
  - [ ] Create GetPhotosForUserQuery (userId, pagination)
  - [ ] Create GetPhotosByTagQuery (tagId, pagination)
  - [ ] Create GetPhotoDetailsQuery (photoId)

- [ ] Task 4: Create DTOs for Read Models (AC: #5, #7)
  - [ ] Create UserDTO (id, email, displayName, emailVerified, createdAt)
  - [ ] Create UserPreferencesDTO (userId, animationsEnabled, soundEnabled, theme, concurrentUploads)
  - [ ] Create UploadSessionDTO (id, userId, status, totalPhotos, completedPhotos, failedPhotos, progressPercentage, createdAt, completedAt)
  - [ ] Create PhotoDTO (id, userId, sessionId, filename, fileSize, uploadStatus, progress, createdAt)
  - [ ] DTOs are immutable records or classes with only getters

- [ ] Task 5: Create Query Handlers (AC: #4, #7)
  - [ ] Create GetUserByIdQueryHandler - fetches User, maps to UserDTO
  - [ ] Create GetUserPreferencesQueryHandler - fetches UserPreferences, maps to UserPreferencesDTO
  - [ ] Create GetUploadSessionQueryHandler - fetches UploadSession, maps to UploadSessionDTO
  - [ ] Create GetActiveSessionsForUserQueryHandler - fetches active sessions, maps to List<UploadSessionDTO>
  - [ ] Create GetPhotosForUserQueryHandler - fetches Photos with pagination, maps to List<PhotoDTO>
  - [ ] Create GetPhotosByTagQueryHandler - fetches Photos by tag, maps to List<PhotoDTO>
  - [ ] Create GetPhotoDetailsQueryHandler - fetches Photo with full details, maps to PhotoDTO
  - [ ] All query handlers are read-only (no @Transactional or use readOnly=true)

- [ ] Task 6: Unit Testing (AC: #9)
  - [ ] Test RegisterUserCommandHandler - verify User created, event published
  - [ ] Test LoginCommandHandler - verify password check, login recorded
  - [ ] Test StartUploadSessionCommandHandler - verify UploadSession created
  - [ ] Test CompletePhotoUploadCommandHandler - verify Photo completed, event published
  - [ ] Test GetUserByIdQueryHandler - verify DTO returned, domain object not leaked
  - [ ] Test GetPhotosForUserQueryHandler - verify pagination works

- [ ] Task 7: Integration Testing (AC: #10)
  - [ ] Test command-query cycle: RegisterUserCommand -> GetUserByIdQuery
  - [ ] Test command-query cycle: StartUploadSessionCommand -> GetActiveSessionsForUserQuery
  - [ ] Test command-query cycle: CompletePhotoUploadCommand -> GetPhotosForUserQuery
  - [ ] Verify domain events published and captured

## Dev Notes

### Architecture Patterns and Constraints

**CQRS (Command Query Responsibility Segregation):**
- **Commands**: Mutate state, return void or ID, transactional
- **Queries**: Read-only, return DTOs, no state changes
- **Command Handlers**: Execute business logic, persist changes, publish domain events
- **Query Handlers**: Fetch data, map to DTOs, no domain object leakage
- **DTOs**: Data Transfer Objects for read models, prevent accidental mutation

**Command Handler Pattern:**
```java
@Service
@Transactional(rollbackFor = Exception.class)
public class RegisterUserCommandHandler {
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UUID handle(RegisterUserCommand command) {
        User user = User.create(Email.of(command.email()), command.password(), command.displayName());
        User saved = userRepository.save(user).block();
        eventPublisher.publishEvent(new UserRegisteredEvent(saved.getId(), saved.getEmail().getValue()));
        return saved.getId();
    }
}
```

**Query Handler Pattern:**
```java
@Service
public class GetUserByIdQueryHandler {
    private final UserRepository userRepository;

    public Mono<UserDTO> handle(GetUserByIdQuery query) {
        return userRepository.findById(query.userId())
            .map(user -> new UserDTO(user.getId(), user.getEmail().getValue(),
                                     user.getDisplayName(), user.isEmailVerified(),
                                     user.getCreatedAt()));
    }
}
```

**DTO Immutability:**
- Use Java records for DTOs (Java 17+)
- Or final classes with only getters
- Never expose domain entities from queries

[Source: docs/epics/epic-0-foundation-infrastructure.md#Story-0.4]

### Source Tree Components to Touch

```
backend/src/main/java/com/rapidphoto/
├── application/
│   ├── commands/
│   │   ├── user/
│   │   │   ├── RegisterUserCommand.java
│   │   │   ├── LoginCommand.java
│   │   │   └── VerifyEmailCommand.java
│   │   ├── upload/
│   │   │   └── StartUploadSessionCommand.java
│   │   └── photo/
│   │       ├── InitiatePhotoUploadCommand.java
│   │       ├── CompletePhotoUploadCommand.java
│   │       ├── AddTagToPhotoCommand.java
│   │       └── RemoveTagFromPhotoCommand.java
│   ├── handlers/
│   │   ├── command/
│   │   │   ├── RegisterUserCommandHandler.java
│   │   │   ├── LoginCommandHandler.java
│   │   │   ├── StartUploadSessionCommandHandler.java
│   │   │   └── CompletePhotoUploadCommandHandler.java
│   │   └── query/
│   │       ├── GetUserByIdQueryHandler.java
│   │       ├── GetPhotosForUserQueryHandler.java
│   │       └── GetUploadSessionQueryHandler.java
│   ├── queries/
│   │   ├── GetUserByIdQuery.java
│   │   ├── GetPhotosForUserQuery.java
│   │   └── GetUploadSessionQuery.java
│   └── dto/
│       ├── UserDTO.java
│       ├── PhotoDTO.java
│       └── UploadSessionDTO.java
└── test/java/com/rapidphoto/application/
    ├── handlers/command/
    │   └── RegisterUserCommandHandlerTest.java
    ├── handlers/query/
    │   └── GetUserByIdQueryHandlerTest.java
    └── integration/
        └── CqrsIntegrationTest.java
```

### Testing Standards Summary

**Command Handler Testing:**
- Mock repositories and event publisher
- Verify domain methods called correctly
- Verify domain events published
- Test transactional rollback on exceptions

**Query Handler Testing:**
- Mock repositories with test data
- Verify DTOs returned (not domain entities)
- Verify pagination works correctly
- Test empty results handled gracefully

**Integration Testing:**
- Execute command
- Verify state persisted to database
- Execute query
- Verify DTO matches expected state

### Project Structure Notes

**Alignment with Unified Project Structure:**
- Application layer (commands/queries/handlers) separate from domain layer
- DTOs in application layer, never in domain layer
- Domain events published from application layer (not domain layer directly)

**Dependencies on Story 0.3:**
- Domain aggregates (User, UploadSession, Photo) from Story 0.3
- Domain events from Story 0.3
- R2DBC repositories from Story 0.3

### Learnings from Previous Story

**From Story 0-3-domain-model-implementation-ddd (Status: review)**

- **New Domain Classes**: User, UploadSession, Photo aggregates with business methods
- **Value Objects**: Email, PhotoId, S3Location available for use
- **Domain Events**: DomainEvent interface, UserRegisteredEvent, UploadCompletedEvent, PhotoUploadedEvent, PhotoFailedEvent
- **R2DBC Repositories**: UserRepository, UploadSessionRepository, PhotoRepository with reactive queries
- **Testing Patterns**: 87 unit tests using JUnit 5, AssertJ, StepVerifier from reactor-test
- **BCrypt Hashing**: User.create() automatically hashes passwords with BCrypt
- **State Machines**: SessionStatus and PhotoStatus enums enforce valid transitions
- **Soft Delete**: Photo.softDelete() method available for undelete feature

**Key Files to Use**:
- Use `backend/src/main/java/com/rapidphoto/domain/**/*.java` for domain aggregates
- Use `backend/src/main/java/com/rapidphoto/domain/**/repository/*.java` for repositories
- Use `backend/src/main/java/com/rapidphoto/domain/events/*.java` for domain events
- Follow test patterns from `backend/src/test/java/com/rapidphoto/domain/**/*Test.java`

[Source: docs/stories/0-3-domain-model-implementation-ddd.md#Dev-Agent-Record]

### References

- Epic Overview: [Source: docs/epics/epic-0-foundation-infrastructure.md#Story-0.4]
- CQRS Pattern: Martin Fowler - CQRS
- Spring ApplicationEventPublisher: Spring Framework documentation

## Dev Agent Record

### Context Reference

<!-- Path(s) to story context XML will be added here by context workflow -->

### Agent Model Used

claude-sonnet-4-5-20250929

### Debug Log References

### Completion Notes List

### File List
