package com.rapidphoto.infrastructure;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests.
 * Uses existing Docker containers (PostgreSQL, Redis, LocalStack S3) for testing.
 * Make sure Docker containers are running before tests: docker-compose up -d
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
    // Tests will use the running Docker containers configured in application-test.yml
}
