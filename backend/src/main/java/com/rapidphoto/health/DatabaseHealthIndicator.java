package com.rapidphoto.health;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Health indicator for PostgreSQL database connectivity.
 * Checks if database is accessible by executing a simple SELECT 1 query.
 */
@Component
public class DatabaseHealthIndicator extends AbstractHealthIndicator {

    private final R2dbcEntityTemplate template;

    public DatabaseHealthIndicator(R2dbcEntityTemplate template) {
        this.template = template;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try {
            // Execute simple query to verify database connection
            template.getDatabaseClient()
                .sql("SELECT 1")
                .fetch()
                .one()
                .block(Duration.ofSeconds(5));

            builder.up()
                .withDetail("database", "PostgreSQL")
                .withDetail("status", "Connected");
        } catch (Exception e) {
            builder.down()
                .withDetail("database", "PostgreSQL")
                .withDetail("error", e.getMessage());
        }
    }
}
