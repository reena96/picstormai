package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.UploadSessionDTO;
import com.rapidphoto.cqrs.queries.GetUploadSessionQuery;
import com.rapidphoto.domain.upload.UploadSessionRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Query handler for GetUploadSessionQuery.
 * Read-only operation - returns DTO, no @Transactional.
 */
@Service
public class GetUploadSessionQueryHandler {

    private final UploadSessionRepository uploadSessionRepository;

    public GetUploadSessionQueryHandler(UploadSessionRepository uploadSessionRepository) {
        this.uploadSessionRepository = uploadSessionRepository;
    }

    public Mono<UploadSessionDTO> handle(GetUploadSessionQuery query) {
        return uploadSessionRepository.findById(query.sessionId())
            .map(UploadSessionDTO::fromDomain)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Upload session not found: " + query.sessionId())));
    }
}
