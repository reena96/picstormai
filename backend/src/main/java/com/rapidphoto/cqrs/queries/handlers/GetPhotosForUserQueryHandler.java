package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.PhotoDTO;
import com.rapidphoto.cqrs.queries.GetPhotosForUserQuery;
import com.rapidphoto.domain.photo.PhotoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Query handler for GetPhotosForUserQuery.
 * Read-only operation with pagination - returns DTOs, no @Transactional.
 */
@Service
public class GetPhotosForUserQueryHandler {

    private final PhotoRepository photoRepository;

    public GetPhotosForUserQueryHandler(PhotoRepository photoRepository) {
        this.photoRepository = photoRepository;
    }

    public Flux<PhotoDTO> handle(GetPhotosForUserQuery query) {
        // Apply pagination using skip and take
        int skip = query.page() * query.size();

        return photoRepository.findByUserId(query.userId())
            .skip(skip)
            .take(query.size())
            .map(PhotoDTO::fromDomain);
    }
}
