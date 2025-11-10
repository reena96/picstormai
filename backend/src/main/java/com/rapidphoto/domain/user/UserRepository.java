package com.rapidphoto.domain.user;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository for User aggregate.
 * Provides reactive CRUD operations and custom queries.
 */
@Repository
public interface UserRepository extends ReactiveCrudRepository<User, UUID> {

    /**
     * Find user by email address.
     */
    @Query("SELECT * FROM users WHERE email = :email")
    Mono<User> findByEmail(String email);

    /**
     * Check if user with email exists.
     */
    @Query("SELECT COUNT(*) > 0 FROM users WHERE email = :email")
    Mono<Boolean> existsByEmail(String email);
}
