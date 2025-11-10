package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.UserDTO;
import com.rapidphoto.cqrs.queries.GetUserByIdQuery;
import com.rapidphoto.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Query handler for GetUserByIdQuery.
 * Read-only operation - returns DTO, no @Transactional.
 */
@Service
public class GetUserByIdQueryHandler {

    private final UserRepository userRepository;

    public GetUserByIdQueryHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Mono<UserDTO> handle(GetUserByIdQuery query) {
        return userRepository.findById(query.userId())
            .map(UserDTO::fromDomain)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found: " + query.userId())));
    }
}
