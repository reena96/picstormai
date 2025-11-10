package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.config.JwtConfig;
import com.rapidphoto.cqrs.commands.LoginCommand;
import com.rapidphoto.cqrs.dtos.LoginResponse;
import com.rapidphoto.domain.refreshtoken.RefreshToken;
import com.rapidphoto.domain.refreshtoken.RefreshTokenRepository;
import com.rapidphoto.domain.shared.Email;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserRepository;
import com.rapidphoto.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginCommandHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private JwtUtil jwtUtil;
    private JwtConfig jwtConfig;
    private LoginCommandHandler handler;

    @BeforeEach
    void setUp() {
        jwtConfig = new JwtConfig();
        ReflectionTestUtils.setField(jwtConfig, "secret", "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256");
        ReflectionTestUtils.setField(jwtConfig, "accessTokenExpirationMinutes", 15L);
        ReflectionTestUtils.setField(jwtConfig, "refreshTokenExpirationDays", 30L);

        jwtUtil = new JwtUtil(jwtConfig);

        handler = new LoginCommandHandler(userRepository, refreshTokenRepository, jwtUtil, jwtConfig, eventPublisher);
    }

    @Test
    void shouldAuthenticateValidUser() {
        // Given
        String email = "test@example.com";
        String password = "password123";
        User user = User.create(Email.of(email), password, "Test User");
        user.verifyEmail(); // Verify email so user can login

        LoginCommand command = new LoginCommand(email, password);

        when(userRepository.findByEmail(email)).thenReturn(Mono.just(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When
        Mono<LoginResponse> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .assertNext(response -> {
                assertThat(response.accessToken()).isNotBlank();
                assertThat(response.refreshToken()).isNotBlank();
                assertThat(response.expiresIn()).isEqualTo(15 * 60); // 15 minutes in seconds
                assertThat(response.tokenType()).isEqualTo("Bearer");
                verify(userRepository).save(any(User.class));
                verify(refreshTokenRepository).save(any(RefreshToken.class));
            })
            .verifyComplete();
    }

    @Test
    void shouldFailWithInvalidPassword() {
        // Given
        String email = "test@example.com";
        User user = User.create(Email.of(email), "correctPassword", "Test User");
        user.verifyEmail(); // Verify email so we test password validation, not email verification

        LoginCommand command = new LoginCommand(email, "wrongPassword");

        when(userRepository.findByEmail(email)).thenReturn(Mono.just(user));

        // When
        Mono<LoginResponse> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Invalid email or password")
            )
            .verify();

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldFailWithNonExistentEmail() {
        // Given
        LoginCommand command = new LoginCommand("nonexistent@example.com", "password123");

        when(userRepository.findByEmail(command.email())).thenReturn(Mono.empty());

        // When
        Mono<LoginResponse> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Invalid email or password")
            )
            .verify();

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void shouldBlockUnverifiedEmailUsers() {
        // Given
        String email = "unverified@example.com";
        String password = "Password123";
        User user = User.create(Email.of(email), password, "Unverified User");
        // User is NOT verified (emailVerified = false by default)

        LoginCommand command = new LoginCommand(email, password);

        when(userRepository.findByEmail(email)).thenReturn(Mono.just(user));

        // When
        Mono<LoginResponse> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Please verify your email")
            )
            .verify();

        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).save(any());
    }
}
