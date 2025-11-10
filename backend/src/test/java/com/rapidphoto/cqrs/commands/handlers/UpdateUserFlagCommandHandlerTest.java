package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.UpdateUserFlagCommand;
import com.rapidphoto.domain.shared.Email;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserFlagCommandHandlerTest {

    @Mock
    private UserRepository userRepository;

    private UpdateUserFlagCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UpdateUserFlagCommandHandler(userRepository);
    }

    @Test
    void shouldMarkOnboardingComplete() {
        // Given
        User user = User.create(Email.of("test@example.com"), "Password123", "Test User");
        UUID userId = user.getId();
        UpdateUserFlagCommand command = new UpdateUserFlagCommand(userId);

        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When
        Mono<User> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .assertNext(updatedUser -> {
                assertThat(updatedUser.isHasSeenOnboarding()).isTrue();
                verify(userRepository).findById(userId);
                verify(userRepository).save(any(User.class));
            })
            .verifyComplete();
    }

    @Test
    void shouldBeIdempotentForAlreadyCompletedOnboarding() {
        // Given
        User user = User.create(Email.of("test@example.com"), "Password123", "Test User");
        user.markOnboardingComplete(); // Already completed
        UUID userId = user.getId();
        UpdateUserFlagCommand command = new UpdateUserFlagCommand(userId);

        when(userRepository.findById(userId)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When
        Mono<User> result = handler.handle(command);

        // Then - Should still succeed for already completed onboarding
        StepVerifier.create(result)
            .assertNext(updatedUser -> {
                assertThat(updatedUser.isHasSeenOnboarding()).isTrue();
                verify(userRepository).findById(userId);
                verify(userRepository).save(any(User.class));
            })
            .verifyComplete();
    }

    @Test
    void shouldRejectNullUserId() {
        // When/Then
        StepVerifier.create(Mono.fromCallable(() -> new UpdateUserFlagCommand(null)))
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    void shouldRejectNonExistentUser() {
        // Given
        UUID userId = UUID.randomUUID();
        UpdateUserFlagCommand command = new UpdateUserFlagCommand(userId);

        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        // When
        Mono<User> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("User not found")
            )
            .verify();

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }
}
