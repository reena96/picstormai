package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.TagDTO;
import com.rapidphoto.cqrs.queries.GetTagsForUserQuery;
import com.rapidphoto.domain.tag.TagRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Handler for GetTagsForUserQuery.
 * Returns all tags created by a user.
 */
@Service
public class GetTagsForUserQueryHandler {

    private final TagRepository tagRepository;

    public GetTagsForUserQueryHandler(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    /**
     * Handle query to get all user's tags.
     *
     * @param query Query containing user ID
     * @return Flux of TagDTO sorted by name
     */
    public Flux<TagDTO> handle(GetTagsForUserQuery query) {
        return tagRepository.findByUserId(query.userId())
            .map(TagDTO::fromDomain);
    }
}
