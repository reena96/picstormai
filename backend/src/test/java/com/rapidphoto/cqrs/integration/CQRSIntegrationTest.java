package com.rapidphoto.cqrs.integration;

import com.rapidphoto.cqrs.commands.RegisterUserCommand;
import com.rapidphoto.cqrs.commands.StartUploadSessionCommand;
import com.rapidphoto.cqrs.commands.handlers.RegisterUserCommandHandler;
import com.rapidphoto.cqrs.commands.handlers.StartUploadSessionCommandHandler;
import com.rapidphoto.cqrs.dtos.UploadSessionDTO;
import com.rapidphoto.cqrs.dtos.UserDTO;
import com.rapidphoto.cqrs.queries.GetActiveSessionsForUserQuery;
import com.rapidphoto.cqrs.queries.GetUserByIdQuery;
import com.rapidphoto.cqrs.queries.handlers.GetActiveSessionsForUserQueryHandler;
import com.rapidphoto.cqrs.queries.handlers.GetUserByIdQueryHandler;
import com.rapidphoto.domain.shared.Email;
import com.rapidphoto.domain.upload.UploadSession;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserPreferences;
import com.rapidphoto.domain.user.UserPreferencesRepository;
import com.rapidphoto.domain.user.UserRepository;
import com.rapidphoto.domain.upload.UploadSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test for CQRS pattern.
 * Tests complete flow: Command -> Domain mutation -> Query -> DTO retrieval.
 */
@ExtendWith(MockitoExtension.class)
class CQRSIntegrationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private UploadSessionRepository uploadSessionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RegisterUserCommandHandler registerUserHandler;
    private GetUserByIdQueryHandler getUserByIdHandler;
    private StartUploadSessionCommandHandler startUploadSessionHandler;
    private GetActiveSessionsForUserQueryHandler getActiveSessionsHandler;

    @BeforeEach
    void setUp() {
        // Initialize handlers
        registerUserHandler = new RegisterUserCommandHandler(userRepository, userPreferencesRepository, eventPublisher);
        getUserByIdHandler = new GetUserByIdQueryHandler(userRepository);
        startUploadSessionHandler = new StartUploadSessionCommandHandler(uploadSessionRepository, eventPublisher);
        getActiveSessionsHandler = new GetActiveSessionsForUserQueryHandler(uploadSessionRepository);
    }

    @Test
    void shouldExecuteRegisterUserCommandAndVerifyWithQuery() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(
            "integration@test.com",
            "password123",
            "Integration Test User"
        );

        User user = User.create(Email.of(command.email()), command.password(), command.displayName());

        when(userRepository.existsByEmail(command.email())).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));

        // When - Execute command
        UUID userId = registerUserHandler.handle(command).block();

        // Then - Verify with query
        GetUserByIdQuery query = new GetUserByIdQuery(userId);

        StepVerifier.create(getUserByIdHandler.handle(query))
            .assertNext(userDTO -> {
                assertThat(userDTO).isNotNull();
                assertThat(userDTO.id()).isEqualTo(userId);
                assertThat(userDTO.email()).isEqualTo("integration@test.com");
                assertThat(userDTO.displayName()).isEqualTo("Integration Test User");
                assertThat(userDTO.emailVerified()).isFalse();
                assertThat(userDTO.createdAt()).isNotNull();
            })
            .verifyComplete();
    }

    @Test
    void shouldExecuteStartUploadSessionCommandAndVerifyWithQuery() {
        // Given
        UUID userId = UUID.randomUUID();
        StartUploadSessionCommand sessionCommand = new StartUploadSessionCommand(userId, 5);
        UploadSession session = UploadSession.start(userId);
        session.setTotalPhotos(5);

        when(uploadSessionRepository.save(any(UploadSession.class))).thenReturn(Mono.just(session));
        when(uploadSessionRepository.findActiveSessionsByUserId(userId)).thenReturn(Flux.just(session));

        // When - Start upload session
        UUID sessionId = startUploadSessionHandler.handle(sessionCommand).block();

        // Then - Verify with query
        GetActiveSessionsForUserQuery query = new GetActiveSessionsForUserQuery(userId);

        StepVerifier.create(getActiveSessionsHandler.handle(query))
            .assertNext(sessionDTO -> {
                assertThat(sessionDTO).isNotNull();
                assertThat(sessionDTO.id()).isEqualTo(sessionId);
                assertThat(sessionDTO.userId()).isEqualTo(userId);
                assertThat(sessionDTO.totalPhotos()).isEqualTo(5);
                assertThat(sessionDTO.completedPhotos()).isEqualTo(0);
                assertThat(sessionDTO.progressPercentage()).isEqualTo(0);
            })
            .verifyComplete();
    }

    @Test
    void shouldVerifyCommandMutationIsVisibleInQuery() {
        // This test ensures that commands actually modify state
        // and queries can immediately see those changes

        // Given
        RegisterUserCommand command1 = new RegisterUserCommand(
            "user1@test.com",
            "password123",
            "User One"
        );

        RegisterUserCommand command2 = new RegisterUserCommand(
            "user2@test.com",
            "password123",
            "User Two"
        );

        User user1 = User.create(Email.of(command1.email()), command1.password(), command1.displayName());
        User user2 = User.create(Email.of(command2.email()), command2.password(), command2.displayName());

        when(userRepository.existsByEmail(command1.email())).thenReturn(Mono.just(false));
        when(userRepository.existsByEmail(command2.email())).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user1), Mono.just(user2));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(userRepository.findById(user1.getId())).thenReturn(Mono.just(user1));
        when(userRepository.findById(user2.getId())).thenReturn(Mono.just(user2));

        // When - Execute multiple commands
        UUID userId1 = registerUserHandler.handle(command1).block();
        UUID userId2 = registerUserHandler.handle(command2).block();

        // Then - Both users should be queryable
        StepVerifier.create(getUserByIdHandler.handle(new GetUserByIdQuery(userId1)))
            .assertNext(dto -> assertThat(dto.email()).isEqualTo("user1@test.com"))
            .verifyComplete();

        StepVerifier.create(getUserByIdHandler.handle(new GetUserByIdQuery(userId2)))
            .assertNext(dto -> assertThat(dto.email()).isEqualTo("user2@test.com"))
            .verifyComplete();
    }

    @Test
    void shouldEnforceDuplicateEmailValidationInCommand() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(
            "duplicate@test.com",
            "password123",
            "First User"
        );

        User user = User.create(Email.of(command.email()), command.password(), command.displayName());

        when(userRepository.existsByEmail(command.email())).thenReturn(Mono.just(false), Mono.just(true));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(user));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        // When - Register first user
        UUID userId = registerUserHandler.handle(command).block();
        assertThat(userId).isNotNull();

        // Then - Attempting to register with same email should fail
        RegisterUserCommand duplicateCommand = new RegisterUserCommand(
            "duplicate@test.com",
            "differentPassword",
            "Second User"
        );

        StepVerifier.create(registerUserHandler.handle(duplicateCommand))
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Email already registered")
            )
            .verify();
    }
}
