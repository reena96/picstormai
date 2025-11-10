package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.RegisterUserCommand;
import com.rapidphoto.domain.events.UserRegisteredEvent;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserPreferences;
import com.rapidphoto.domain.user.UserPreferencesRepository;
import com.rapidphoto.domain.user.UserRepository;
import com.rapidphoto.domain.verification.EmailVerificationToken;
import com.rapidphoto.domain.verification.EmailVerificationTokenRepository;
import com.rapidphoto.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserCommandHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private EmailVerificationTokenRepository verificationTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RegisterUserCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RegisterUserCommandHandler(
            userRepository,
            userPreferencesRepository,
            verificationTokenRepository,
            emailService,
            eventPublisher
        );
    }

    @Test
    void shouldRegisterNewUser() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(
            "test@example.com",
            "Password123", // Valid password with uppercase and number
            "Test User"
        );

        when(userRepository.existsByEmail(command.email())).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(verificationTokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(emailService.sendVerificationEmail(anyString(), anyString(), anyString())).thenReturn(Mono.empty());

        // When
        Mono<UUID> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .assertNext(userId -> {
                assertThat(userId).isNotNull();
                verify(userRepository).save(any(User.class));
                verify(userPreferencesRepository).save(any(UserPreferences.class));
                verify(verificationTokenRepository).save(any(EmailVerificationToken.class));
                verify(emailService).sendVerificationEmail(anyString(), anyString(), anyString());
                verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
            })
            .verifyComplete();
    }

    @Test
    void shouldFailWhenEmailAlreadyExists() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(
            "existing@example.com",
            "Password123",
            "Test User"
        );

        when(userRepository.existsByEmail(command.email())).thenReturn(Mono.just(true));

        // When
        Mono<UUID> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Email already registered")
            )
            .verify();

        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void shouldPublishUserRegisteredEvent() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(
            "test@example.com",
            "Password123",
            "Test User"
        );

        when(userRepository.existsByEmail(command.email())).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(verificationTokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(emailService.sendVerificationEmail(anyString(), anyString(), anyString())).thenReturn(Mono.empty());

        // When
        handler.handle(command).block();

        // Then
        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        UserRegisteredEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isNotNull();
        assertThat(event.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void shouldRejectWeakPassword() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(
            "test@example.com",
            "weak", // Password too short, no uppercase, no number
            "Test User"
        );

        // When
        Mono<UUID> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Password")
            )
            .verify();

        verify(userRepository, never()).save(any());
    }
}
