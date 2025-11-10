package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.PhotoDTO;
import com.rapidphoto.cqrs.queries.GetPhotosByTagQuery;
import com.rapidphoto.domain.photo.PhotoRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Query handler for GetPhotosByTagQuery.
 * Read-only operation with pagination - returns DTOs, no @Transactional.
 */
@Service
public class GetPhotosByTagQueryHandler {

    private final PhotoRepository photoRepository;

    public GetPhotosByTagQueryHandler(PhotoRepository photoRepository) {
        this.photoRepository = photoRepository;
    }

    public Flux<PhotoDTO> handle(GetPhotosByTagQuery query) {
        // Apply pagination using skip and take
        int skip = query.page() * query.size();

        return photoRepository.findByTagId(query.tagId())
            .filter(photo -> photo.getUserId().equals(query.userId())) // Filter by user
            .skip(skip)
            .take(query.size())
            .map(PhotoDTO::fromDomain);
    }
}
