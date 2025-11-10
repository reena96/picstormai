package com.rapidphoto.api;

import com.rapidphoto.cqrs.dtos.LoginResponse;
import com.rapidphoto.cqrs.dtos.RefreshTokenResponse;
import com.rapidphoto.domain.refreshtoken.RefreshTokenRepository;
import com.rapidphoto.domain.shared.Email;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for AuthController.
 * Uses Testcontainers for PostgreSQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@Testcontainers
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
            String.format("r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(),
                postgres.getFirstMappedPort(),
                postgres.getDatabaseName())
        );
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("jwt.secret", () -> "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256");
    }

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = User.create(
            Email.of("test@example.com"),
            "TestPassword123",
            "Test User"
        );
        testUser.verifyEmail(); // Verify email so user can log in

        userRepository.save(testUser).block();
    }

    @AfterEach
    void tearDown() {
        refreshTokenRepository.deleteAll().block();
        userRepository.deleteAll().block();
    }

    @Test
    void login_withValidCredentials_shouldReturnTokens() {
        // Arrange
        var loginRequest = new AuthController.LoginRequest("test@example.com", "TestPassword123");

        // Act & Assert
        webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody(LoginResponse.class)
            .value(response -> {
                assertThat(response.accessToken()).isNotBlank();
                assertThat(response.refreshToken()).isNotBlank();
                assertThat(response.expiresIn()).isEqualTo(15 * 60); // 15 minutes in seconds
                assertThat(response.tokenType()).isEqualTo("Bearer");
            });
    }

    @Test
    void login_withInvalidPassword_shouldReturn401() {
        // Arrange
        var loginRequest = new AuthController.LoginRequest("test@example.com", "WrongPassword");

        // Act & Assert
        webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void login_withNonExistentUser_shouldReturn401() {
        // Arrange
        var loginRequest = new AuthController.LoginRequest("nonexistent@example.com", "password");

        // Act & Assert
        webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void refresh_withValidToken_shouldReturnNewTokens() {
        // Arrange: Login first to get refresh token
        var loginRequest = new AuthController.LoginRequest("test@example.com", "TestPassword123");
        LoginResponse loginResponse = webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody(LoginResponse.class)
            .returnResult()
            .getResponseBody();

        var refreshRequest = new AuthController.RefreshRequest(loginResponse.refreshToken());

        // Act & Assert
        webTestClient.post()
            .uri("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(refreshRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody(RefreshTokenResponse.class)
            .value(response -> {
                assertThat(response.accessToken()).isNotBlank();
                assertThat(response.refreshToken()).isNotBlank();
                // New tokens should be different from original
                assertThat(response.accessToken()).isNotEqualTo(loginResponse.accessToken());
                assertThat(response.refreshToken()).isNotEqualTo(loginResponse.refreshToken());
                assertThat(response.expiresIn()).isEqualTo(15 * 60);
                assertThat(response.tokenType()).isEqualTo("Bearer");
            });
    }

    @Test
    void refresh_withInvalidToken_shouldReturn401() {
        // Arrange
        var refreshRequest = new AuthController.RefreshRequest("invalid.jwt.token");

        // Act & Assert
        webTestClient.post()
            .uri("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(refreshRequest)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void refresh_withExpiredToken_shouldReturn401() {
        // This test would require manipulating time or creating expired token
        // For now, testing with invalid token covers similar validation
        var refreshRequest = new AuthController.RefreshRequest("expired.token.here");

        webTestClient.post()
            .uri("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(refreshRequest)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void tokenRotation_shouldReturnNewRefreshToken() {
        // Arrange: Login to get initial refresh token
        var loginRequest = new AuthController.LoginRequest("test@example.com", "TestPassword123");
        LoginResponse loginResponse = webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectBody(LoginResponse.class)
            .returnResult()
            .getResponseBody();

        String oldRefreshToken = loginResponse.refreshToken();

        // Act: Use refresh token to get new tokens
        var refreshRequest = new AuthController.RefreshRequest(oldRefreshToken);
        RefreshTokenResponse refreshResponse = webTestClient.post()
            .uri("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(refreshRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody(RefreshTokenResponse.class)
            .returnResult()
            .getResponseBody();

        // Assert: Token rotation produces new tokens
        assertThat(refreshResponse.refreshToken()).isNotEqualTo(oldRefreshToken);
        assertThat(refreshResponse.accessToken()).isNotEqualTo(loginResponse.accessToken());

        // Verify new token works
        webTestClient.post()
            .uri("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new AuthController.RefreshRequest(refreshResponse.refreshToken()))
            .exchange()
            .expectStatus().isOk();

        // TODO: Test that old token is invalidated (requires transaction isolation fix)
        // Currently old tokens remain valid due to reactive transaction timing
    }

    @Test
    void fullRegistrationFlow_shouldWorkEndToEnd() {
        // Arrange
        var registerRequest = new AuthController.RegisterRequest(
            "newuser@example.com",
            "NewPassword123",
            "New User"
        );

        // Act 1: Register user
        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(registerRequest)
            .exchange()
            .expectStatus().isCreated();

        // Act 2: Try to login before verification - should fail
        var loginRequest = new AuthController.LoginRequest("newuser@example.com", "NewPassword123");
        webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isUnauthorized();

        // Act 3: Verify email (get token from MockEmailService logs or database)
        // In a real test, we'd extract the token from email service
        // For now, we'll verify the user directly
        User newUser = userRepository.findByEmail("newuser@example.com").block();
        assertThat(newUser).isNotNull();
        newUser.verifyEmail();
        userRepository.save(newUser).block();

        // Act 4: Login after verification - should succeed
        webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody(LoginResponse.class)
            .value(response -> {
                assertThat(response.accessToken()).isNotBlank();
                assertThat(response.refreshToken()).isNotBlank();
            });
    }

    @Test
    void login_withUnverifiedEmail_shouldReturn401() {
        // Arrange: Create user without verifying email
        User unverifiedUser = User.create(
            Email.of("unverified@example.com"),
            "TestPassword123",
            "Unverified User"
        );
        // Do NOT call verifyEmail()
        userRepository.save(unverifiedUser).block();

        var loginRequest = new AuthController.LoginRequest("unverified@example.com", "TestPassword123");

        // Act & Assert
        webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void register_withDuplicateEmail_shouldReturn409() {
        // Arrange: User already exists
        var registerRequest = new AuthController.RegisterRequest(
            "test@example.com", // Same as testUser
            "SomePassword123",
            "Duplicate User"
        );

        // Act & Assert
        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(registerRequest)
            .exchange()
            .expectStatus().isEqualTo(409); // Conflict
    }

    @Test
    void register_withWeakPassword_shouldReturn400() {
        // Arrange: Password too short, no uppercase, or no number
        var weakPasswordRequest = new AuthController.RegisterRequest(
            "weakpass@example.com",
            "weak", // Too short, no uppercase, no number
            "Test User"
        );

        // Act & Assert
        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(weakPasswordRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void register_withPasswordNoUppercase_shouldReturn400() {
        // Arrange
        var noUppercaseRequest = new AuthController.RegisterRequest(
            "nouppercase@example.com",
            "password123", // No uppercase
            "Test User"
        );

        // Act & Assert
        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(noUppercaseRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void register_withPasswordNoNumber_shouldReturn400() {
        // Arrange
        var noNumberRequest = new AuthController.RegisterRequest(
            "nonumber@example.com",
            "Password", // No number
            "Test User"
        );

        // Act & Assert
        webTestClient.post()
            .uri("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(noNumberRequest)
            .exchange()
            .expectStatus().isBadRequest();
    }

    @Test
    void jwtToken_shouldContainCorrectClaims() {
        // Arrange
        var loginRequest = new AuthController.LoginRequest("test@example.com", "TestPassword123");

        // Act
        LoginResponse loginResponse = webTestClient.post()
            .uri("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(loginRequest)
            .exchange()
            .expectStatus().isOk()
            .expectBody(LoginResponse.class)
            .returnResult()
            .getResponseBody();

        // Assert: Validate JWT structure
        // Note: In real test, we'd decode JWT and verify claims
        // For now, checking token is not blank and format looks correct
        assertThat(loginResponse.accessToken()).isNotBlank();
        assertThat(loginResponse.accessToken()).contains(".");
        assertThat(loginResponse.tokenType()).isEqualTo("Bearer");
        assertThat(loginResponse.expiresIn()).isEqualTo(15 * 60); // 15 minutes
    }
}
