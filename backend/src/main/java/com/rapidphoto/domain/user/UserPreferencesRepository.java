package com.rapidphoto.domain.user;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Repository for UserPreferences entity.
 */
@Repository
public interface UserPreferencesRepository extends ReactiveCrudRepository<UserPreferences, UUID> {

    /**
     * Find preferences by user ID.
     */
    @Query("SELECT * FROM user_preferences WHERE user_id = :userId")
    Mono<UserPreferences> findByUserId(UUID userId);
}
