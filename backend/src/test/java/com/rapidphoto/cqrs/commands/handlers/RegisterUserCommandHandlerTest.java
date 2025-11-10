package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.RegisterUserCommand;
import com.rapidphoto.domain.events.UserRegisteredEvent;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserPreferences;
import com.rapidphoto.domain.user.UserPreferencesRepository;
import com.rapidphoto.domain.user.UserRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserCommandHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferencesRepository userPreferencesRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private RegisterUserCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RegisterUserCommandHandler(userRepository, userPreferencesRepository, eventPublisher);
    }

    @Test
    void shouldRegisterNewUser() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(
            "test@example.com",
            "password123",
            "Test User"
        );

        when(userRepository.existsByEmail(command.email())).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When
        Mono<UUID> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .assertNext(userId -> {
                assertThat(userId).isNotNull();
                verify(userRepository).save(any(User.class));
                verify(userPreferencesRepository).save(any(UserPreferences.class));
                verify(eventPublisher).publishEvent(any(UserRegisteredEvent.class));
            })
            .verifyComplete();
    }

    @Test
    void shouldFailWhenEmailAlreadyExists() {
        // Given
        RegisterUserCommand command = new RegisterUserCommand(
            "existing@example.com",
            "password123",
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
            "password123",
            "Test User"
        );

        when(userRepository.existsByEmail(command.email())).thenReturn(Mono.just(false));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(userPreferencesRepository.save(any(UserPreferences.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When
        handler.handle(command).block();

        // Then
        ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        UserRegisteredEvent event = eventCaptor.getValue();
        assertThat(event.getUserId()).isNotNull();
        assertThat(event.getEmail()).isEqualTo("test@example.com");
    }
}
