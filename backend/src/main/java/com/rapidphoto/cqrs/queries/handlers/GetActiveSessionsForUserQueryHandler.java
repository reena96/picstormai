package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.UploadSessionDTO;
import com.rapidphoto.cqrs.queries.GetActiveSessionsForUserQuery;
import com.rapidphoto.domain.upload.UploadSessionRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Query handler for GetActiveSessionsForUserQuery.
 * Read-only operation - returns DTOs, no @Transactional.
 */
@Service
public class GetActiveSessionsForUserQueryHandler {

    private final UploadSessionRepository uploadSessionRepository;

    public GetActiveSessionsForUserQueryHandler(UploadSessionRepository uploadSessionRepository) {
        this.uploadSessionRepository = uploadSessionRepository;
    }

    public Flux<UploadSessionDTO> handle(GetActiveSessionsForUserQuery query) {
        return uploadSessionRepository.findActiveSessionsByUserId(query.userId())
            .map(UploadSessionDTO::fromDomain);
    }
}
