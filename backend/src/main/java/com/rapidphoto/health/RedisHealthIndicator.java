package com.rapidphoto.health;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Health indicator for Redis connectivity.
 * Checks if Redis is accessible by executing a simple operation.
 */
@Component
public class RedisHealthIndicator extends AbstractHealthIndicator {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    public RedisHealthIndicator(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try {
            // Test Redis connection by attempting a simple operation
            String testKey = "_health_check_";
            String testValue = "ping";

            redisTemplate.opsForValue()
                .set(testKey, testValue, Duration.ofSeconds(1))
                .then(redisTemplate.delete(testKey))
                .block(Duration.ofSeconds(5));

            builder.up()
                .withDetail("redis", "Connected")
                .withDetail("ping", "PONG");
        } catch (Exception e) {
            builder.down()
                .withDetail("redis", "Connection failed")
                .withDetail("error", e.getMessage());
        }
    }
}
