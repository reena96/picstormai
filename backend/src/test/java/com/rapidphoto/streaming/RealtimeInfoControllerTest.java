package com.rapidphoto.streaming;

import com.rapidphoto.infrastructure.BaseIntegrationTest;
import com.rapidphoto.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for RealtimeInfoController.
 * Story 2.6: Real-Time Streaming Infrastructure Setup
 */
class RealtimeInfoControllerTest extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    void shouldReturnRealtimeInfo() {
        // Given - Valid JWT token
        UUID userId = UUID.randomUUID();
        String token = jwtUtil.generateAccessToken(userId, "test@example.com");

        // When - Request realtime info
        webTestClient
            .get()
            .uri("/api/realtime/info")
            .header("Authorization", "Bearer " + token)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.sessionStreamEndpoint").isEqualTo("/api/upload/sessions/{sessionId}/stream")
            .jsonPath("$.protocol").isEqualTo("Server-Sent Events (SSE)")
            .jsonPath("$.messageTypes").isArray()
            .jsonPath("$.messageTypes[0]").value(msg ->
                assertThat(msg.toString()).contains("PHOTO_UPLOADED"))
            .jsonPath("$.messageTypes[1]").value(msg ->
                assertThat(msg.toString()).contains("PHOTO_FAILED"))
            .jsonPath("$.messageTypes[2]").value(msg ->
                assertThat(msg.toString()).contains("SESSION_COMPLETED"))
            .jsonPath("$.notificationStreamEndpoint").isEqualTo("/api/upload/sessions/notifications/stream")
            .jsonPath("$.connectionInfo").isArray()
            .jsonPath("$.connectionInfo[0]").value(info ->
                assertThat(info.toString()).contains("Authentication required"));
    }
}
