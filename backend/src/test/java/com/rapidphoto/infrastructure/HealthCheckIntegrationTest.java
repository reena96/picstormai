package com.rapidphoto.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for Spring Boot Actuator health checks.
 * Tests /actuator/health endpoint and verifies all components report healthy.
 */
@AutoConfigureWebTestClient
class HealthCheckIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldReturnHealthStatusUP() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void shouldIncludeDatabaseHealthStatus() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.components.database").exists()
            .jsonPath("$.components.database.status").isEqualTo("UP")
            .jsonPath("$.components.database.details.database").isEqualTo("PostgreSQL");
    }

    @Test
    void shouldIncludeRedisHealthStatus() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.components.redis").exists()
            .jsonPath("$.components.redis.status").isEqualTo("UP")
            .jsonPath("$.components.redis.details.ping").isEqualTo("PONG");
    }

    @Test
    void shouldIncludeS3HealthStatus() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.components.s3").exists()
            .jsonPath("$.components.s3.status").isEqualTo("UP");
    }

    @Test
    void shouldShowAllComponentsHealthy() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.status").isEqualTo("UP")
            .jsonPath("$.components.database.status").isEqualTo("UP")
            .jsonPath("$.components.redis.status").isEqualTo("UP")
            .jsonPath("$.components.s3.status").isEqualTo("UP");
    }

    @Test
    void shouldProvideDetailedHealthInformation() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.components").exists()
            .jsonPath("$.components.database").exists()
            .jsonPath("$.components.redis").exists()
            .jsonPath("$.components.s3").exists()
            .jsonPath("$.components.database.details").exists()
            .jsonPath("$.components.redis.details").exists()
            .jsonPath("$.components.s3.details").exists();
    }
}
