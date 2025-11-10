package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.LoginCommand;
import com.rapidphoto.domain.shared.Email;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class LoginCommandHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private LoginCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new LoginCommandHandler(userRepository, eventPublisher);
    }

    @Test
    void shouldAuthenticateValidUser() {
        // Given
        String email = "test@example.com";
        String password = "password123";
        User user = User.create(Email.of(email), password, "Test User");

        LoginCommand command = new LoginCommand(email, password);

        when(userRepository.findByEmail(email)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When
        Mono<UUID> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .assertNext(userId -> {
                assertThat(userId).isEqualTo(user.getId());
                verify(userRepository).save(any(User.class));
            })
            .verifyComplete();
    }

    @Test
    void shouldFailWithInvalidPassword() {
        // Given
        String email = "test@example.com";
        User user = User.create(Email.of(email), "correctPassword", "Test User");

        LoginCommand command = new LoginCommand(email, "wrongPassword");

        when(userRepository.findByEmail(email)).thenReturn(Mono.just(user));

        // When
        Mono<UUID> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Invalid email or password")
            )
            .verify();

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldFailWithNonExistentEmail() {
        // Given
        LoginCommand command = new LoginCommand("nonexistent@example.com", "password123");

        when(userRepository.findByEmail(command.email())).thenReturn(Mono.empty());

        // When
        Mono<UUID> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Invalid email or password")
            )
            .verify();
    }
}
