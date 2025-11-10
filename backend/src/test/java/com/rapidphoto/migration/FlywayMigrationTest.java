package com.rapidphoto.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Flyway database migrations.
 * Uses Testcontainers to spin up a real PostgreSQL database.
 */
@SpringBootTest
@Testcontainers
class FlywayMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("rapidphoto_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + postgres.getHost() + ":" + postgres.getFirstMappedPort() + "/" + postgres.getDatabaseName());
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Test
    void testFlywayMigrationsExecuteSuccessfully() throws SQLException {
        // Given: Flyway is configured
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();

        // When: Migrations are executed
        int migrationsApplied = flyway.migrate().migrationsExecuted;

        // Then: All 4 migrations should be applied
        assertEquals(4, migrationsApplied, "Expected 4 migrations to be applied (V1, V2, V3, V4)");
    }

    @Test
    void testAllTablesCreated() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Expected tables
            String[] expectedTables = {
                    "users", "user_preferences", "refresh_tokens",
                    "upload_sessions", "photos",
                    "tags", "photo_tags"
            };

            for (String tableName : expectedTables) {
                ResultSet rs = stmt.executeQuery(
                        "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = '" + tableName + "')");
                rs.next();
                assertTrue(rs.getBoolean(1), "Table " + tableName + " should exist");
            }
        }
    }

    @Test
    void testUserTableConstraints() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Test unique constraint on email
            stmt.execute("INSERT INTO users (email, password_hash, display_name) VALUES ('test@example.com', 'hash123', 'Test User')");

            assertThrows(SQLException.class, () -> {
                try (Statement stmt2 = conn.createStatement()) {
                    stmt2.execute("INSERT INTO users (email, password_hash, display_name) VALUES ('test@example.com', 'hash456', 'Another User')");
                }
            }, "Duplicate email should violate unique constraint");

            // Test email format check constraint
            assertThrows(SQLException.class, () -> {
                try (Statement stmt2 = conn.createStatement()) {
                    stmt2.execute("INSERT INTO users (email, password_hash, display_name) VALUES ('invalid-email', 'hash789', 'Invalid User')");
                }
            }, "Invalid email format should violate check constraint");
        }
    }

    @Test
    void testForeignKeyConstraints() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Insert user first
            stmt.execute("INSERT INTO users (id, email, password_hash, display_name) VALUES ('550e8400-e29b-41d4-a716-446655440000', 'fk@example.com', 'hash', 'FK User')");

            // Test foreign key constraint: photo must reference existing user
            assertThrows(SQLException.class, () -> {
                try (Statement stmt2 = conn.createStatement()) {
                    stmt2.execute("INSERT INTO photos (user_id, filename, s3_key) VALUES ('550e8400-e29b-41d4-a716-446655440099', 'test.jpg', 'key/test.jpg')");
                }
            }, "Photo with non-existent user_id should violate foreign key constraint");

            // Valid foreign key should work
            stmt.execute("INSERT INTO photos (user_id, filename, s3_key) VALUES ('550e8400-e29b-41d4-a716-446655440000', 'test.jpg', 'key/test.jpg')");

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM photos WHERE user_id = '550e8400-e29b-41d4-a716-446655440000'");
            rs.next();
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void testCheckConstraints() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Insert user first
            stmt.execute("INSERT INTO users (id, email, password_hash, display_name) VALUES ('550e8400-e29b-41d4-a716-446655440001', 'check@example.com', 'hash', 'Check User')");

            // Test concurrent_uploads range check (must be 1-10)
            assertThrows(SQLException.class, () -> {
                try (Statement stmt2 = conn.createStatement()) {
                    stmt2.execute("INSERT INTO user_preferences (user_id, concurrent_uploads) VALUES ('550e8400-e29b-41d4-a716-446655440001', 0)");
                }
            }, "concurrent_uploads = 0 should violate check constraint");

            assertThrows(SQLException.class, () -> {
                try (Statement stmt2 = conn.createStatement()) {
                    stmt2.execute("INSERT INTO user_preferences (user_id, concurrent_uploads) VALUES ('550e8400-e29b-41d4-a716-446655440001', 11)");
                }
            }, "concurrent_uploads = 11 should violate check constraint");

            // Valid value should work
            stmt.execute("INSERT INTO user_preferences (user_id, concurrent_uploads) VALUES ('550e8400-e29b-41d4-a716-446655440001', 5)");

            ResultSet rs = stmt.executeQuery("SELECT concurrent_uploads FROM user_preferences WHERE user_id = '550e8400-e29b-41d4-a716-446655440001'");
            rs.next();
            assertEquals(5, rs.getInt(1));
        }
    }

    @Test
    void testIndexesCreated() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Check critical indexes exist
            String[] expectedIndexes = {
                    "idx_users_email",
                    "idx_photos_user_id_created_at",
                    "idx_photos_s3_key",
                    "idx_upload_sessions_user_id_created_at",
                    "idx_tags_user_id_name",
                    "idx_photo_tags_tag_id_photo_id"
            };

            for (String indexName : expectedIndexes) {
                ResultSet rs = stmt.executeQuery(
                        "SELECT EXISTS (SELECT FROM pg_indexes WHERE indexname = '" + indexName + "')");
                rs.next();
                assertTrue(rs.getBoolean(1), "Index " + indexName + " should exist");
            }
        }
    }

    @Test
    void testCascadeDeleteBehavior() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Insert user
            stmt.execute("INSERT INTO users (id, email, password_hash, display_name) VALUES ('550e8400-e29b-41d4-a716-446655440002', 'cascade@example.com', 'hash', 'Cascade User')");

            // Insert preferences
            stmt.execute("INSERT INTO user_preferences (user_id) VALUES ('550e8400-e29b-41d4-a716-446655440002')");

            // Insert photo
            stmt.execute("INSERT INTO photos (user_id, filename, s3_key) VALUES ('550e8400-e29b-41d4-a716-446655440002', 'cascade.jpg', 'key/cascade.jpg')");

            // Delete user (should cascade to preferences and photos)
            stmt.execute("DELETE FROM users WHERE id = '550e8400-e29b-41d4-a716-446655440002'");

            // Verify cascade deletes
            ResultSet rs1 = stmt.executeQuery("SELECT COUNT(*) FROM user_preferences WHERE user_id = '550e8400-e29b-41d4-a716-446655440002'");
            rs1.next();
            assertEquals(0, rs1.getInt(1), "User preferences should be cascade deleted");

            ResultSet rs2 = stmt.executeQuery("SELECT COUNT(*) FROM photos WHERE user_id = '550e8400-e29b-41d4-a716-446655440002'");
            rs2.next();
            assertEquals(0, rs2.getInt(1), "Photos should be cascade deleted");
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }
}
