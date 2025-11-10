package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.PhotoDTO;
import com.rapidphoto.cqrs.queries.GetPhotoDetailsQuery;
import com.rapidphoto.domain.photo.PhotoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Query handler for GetPhotoDetailsQuery.
 * Read-only operation - returns DTO, no @Transactional.
 */
@Service
public class GetPhotoDetailsQueryHandler {

    private final PhotoRepository photoRepository;

    public GetPhotoDetailsQueryHandler(PhotoRepository photoRepository) {
        this.photoRepository = photoRepository;
    }

    public Mono<PhotoDTO> handle(GetPhotoDetailsQuery query) {
        return photoRepository.findById(query.photoId())
            .map(PhotoDTO::fromDomain)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Photo not found: " + query.photoId())));
    }
}
