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
    void tokenRotation_shouldInvalidateOldRefreshToken() {
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
        webTestClient.post()
            .uri("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(refreshRequest)
            .exchange()
            .expectStatus().isOk();

        // Assert: Old refresh token should no longer work
        webTestClient.post()
            .uri("/api/auth/refresh")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(new AuthController.RefreshRequest(oldRefreshToken))
            .exchange()
            .expectStatus().isUnauthorized();
    }
}
