package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.UserPreferencesDTO;
import com.rapidphoto.cqrs.queries.GetUserPreferencesQuery;
import com.rapidphoto.domain.user.UserPreferencesRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Query handler for GetUserPreferencesQuery.
 * Read-only operation - returns DTO, no @Transactional.
 */
@Service
public class GetUserPreferencesQueryHandler {

    private final UserPreferencesRepository userPreferencesRepository;

    public GetUserPreferencesQueryHandler(UserPreferencesRepository userPreferencesRepository) {
        this.userPreferencesRepository = userPreferencesRepository;
    }

    public Mono<UserPreferencesDTO> handle(GetUserPreferencesQuery query) {
        return userPreferencesRepository.findByUserId(query.userId())
            .map(UserPreferencesDTO::fromDomain)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("User preferences not found: " + query.userId())));
    }
}
