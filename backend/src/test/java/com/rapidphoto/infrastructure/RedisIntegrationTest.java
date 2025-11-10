package com.rapidphoto.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Redis.
 * Tests connection, session storage, TTL, and basic operations.
 */
class RedisIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

    @Test
    void shouldConnectToRedis() {
        // Test PING command
        StepVerifier.create(
            redisTemplate.getConnectionFactory()
                .getReactiveConnection()
                .serverCommands()
                .ping()
        )
            .assertNext(response -> assertThat(response).isEqualTo("PONG"))
            .verifyComplete();
    }

    @Test
    void shouldStoreAndRetrieveSession() {
        String sessionKey = "session:" + UUID.randomUUID();
        String sessionValue = "{\"userId\":\"123\",\"active\":true}";

        // Store session
        StepVerifier.create(
            redisTemplate.opsForValue().set(sessionKey, sessionValue)
        )
            .assertNext(success -> assertThat(success).isTrue())
            .verifyComplete();

        // Retrieve session
        StepVerifier.create(
            redisTemplate.opsForValue().get(sessionKey)
        )
            .assertNext(value -> assertThat(value).isEqualTo(sessionValue))
            .verifyComplete();

        // Cleanup
        StepVerifier.create(redisTemplate.delete(sessionKey))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void shouldRespectTTLExpiration() {
        String key = "ttl-test:" + UUID.randomUUID();
        String value = "expires-soon";

        // Set key with 2 second TTL
        StepVerifier.create(
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(2))
        )
            .assertNext(success -> assertThat(success).isTrue())
            .verifyComplete();

        // Verify key exists immediately
        StepVerifier.create(redisTemplate.hasKey(key))
            .assertNext(exists -> assertThat(exists).isTrue())
            .verifyComplete();

        // Wait for expiration
        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify key has expired
        StepVerifier.create(redisTemplate.hasKey(key))
            .assertNext(exists -> assertThat(exists).isFalse())
            .verifyComplete();
    }

    @Test
    void shouldUpdateExistingSession() {
        String sessionKey = "session:" + UUID.randomUUID();
        String initialValue = "{\"status\":\"active\"}";
        String updatedValue = "{\"status\":\"completed\"}";

        // Set initial value
        StepVerifier.create(
            redisTemplate.opsForValue().set(sessionKey, initialValue)
        )
            .assertNext(success -> assertThat(success).isTrue())
            .verifyComplete();

        // Update value
        StepVerifier.create(
            redisTemplate.opsForValue().set(sessionKey, updatedValue)
        )
            .assertNext(success -> assertThat(success).isTrue())
            .verifyComplete();

        // Verify updated value
        StepVerifier.create(
            redisTemplate.opsForValue().get(sessionKey)
        )
            .assertNext(value -> assertThat(value).isEqualTo(updatedValue))
            .verifyComplete();

        // Cleanup
        StepVerifier.create(redisTemplate.delete(sessionKey))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void shouldDeleteSession() {
        String sessionKey = "session:" + UUID.randomUUID();
        String sessionValue = "{\"userId\":\"456\"}";

        // Create session
        StepVerifier.create(
            redisTemplate.opsForValue().set(sessionKey, sessionValue)
        )
            .expectNext(true)
            .verifyComplete();

        // Verify exists
        StepVerifier.create(redisTemplate.hasKey(sessionKey))
            .expectNext(true)
            .verifyComplete();

        // Delete session
        StepVerifier.create(redisTemplate.delete(sessionKey))
            .expectNext(1L)
            .verifyComplete();

        // Verify deleted
        StepVerifier.create(redisTemplate.hasKey(sessionKey))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void shouldHandleMultipleConcurrentSessions() {
        String session1Key = "session:" + UUID.randomUUID();
        String session2Key = "session:" + UUID.randomUUID();
        String session3Key = "session:" + UUID.randomUUID();

        // Store multiple sessions
        StepVerifier.create(
            redisTemplate.opsForValue().set(session1Key, "session1")
                .then(redisTemplate.opsForValue().set(session2Key, "session2"))
                .then(redisTemplate.opsForValue().set(session3Key, "session3"))
        )
            .verifyComplete();

        // Verify all exist
        StepVerifier.create(redisTemplate.hasKey(session1Key))
            .expectNext(true)
            .verifyComplete();

        StepVerifier.create(redisTemplate.hasKey(session2Key))
            .expectNext(true)
            .verifyComplete();

        StepVerifier.create(redisTemplate.hasKey(session3Key))
            .expectNext(true)
            .verifyComplete();

        // Cleanup
        StepVerifier.create(
            redisTemplate.delete(session1Key)
                .then(redisTemplate.delete(session2Key))
                .then(redisTemplate.delete(session3Key))
        )
            .verifyComplete();
    }

    @Test
    void shouldGetTTLForKey() {
        String key = "ttl-check:" + UUID.randomUUID();
        String value = "test-value";

        // Set key with 60 second TTL
        StepVerifier.create(
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(60))
        )
            .expectNext(true)
            .verifyComplete();

        // Get TTL
        StepVerifier.create(
            redisTemplate.getExpire(key)
        )
            .assertNext(ttl -> {
                assertThat(ttl).isGreaterThan(Duration.ofSeconds(50));
                assertThat(ttl).isLessThanOrEqualTo(Duration.ofSeconds(60));
            })
            .verifyComplete();

        // Cleanup
        StepVerifier.create(redisTemplate.delete(key))
            .expectNextCount(1)
            .verifyComplete();
    }
}
