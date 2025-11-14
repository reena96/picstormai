package com.rapidphoto.api;

import com.rapidphoto.cqrs.commands.AddTagToPhotoCommand;
import com.rapidphoto.cqrs.commands.RemoveTagFromPhotoCommand;
import com.rapidphoto.cqrs.commands.handlers.AddTagToPhotoCommandHandler;
import com.rapidphoto.cqrs.commands.handlers.RemoveTagFromPhotoCommandHandler;
import com.rapidphoto.cqrs.dtos.TagDTO;
import com.rapidphoto.cqrs.queries.GetTagsForUserQuery;
import com.rapidphoto.cqrs.queries.handlers.GetTagsForUserQueryHandler;
import com.rapidphoto.security.CurrentUser;
import com.rapidphoto.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Tag Controller - Photo Tagging and Filtering
 * Story 3.3: Photo Tagging API
 * Story 3.4: Tag Filter API
 */
@RestController
@RequestMapping("/api")
public class TagController {

    private final AddTagToPhotoCommandHandler addTagHandler;
    private final RemoveTagFromPhotoCommandHandler removeTagHandler;
    private final GetTagsForUserQueryHandler getTagsHandler;

    public TagController(
        AddTagToPhotoCommandHandler addTagHandler,
        RemoveTagFromPhotoCommandHandler removeTagHandler,
        GetTagsForUserQueryHandler getTagsHandler
    ) {
        this.addTagHandler = addTagHandler;
        this.removeTagHandler = removeTagHandler;
        this.getTagsHandler = getTagsHandler;
    }

    /**
     * POST /api/photos/{photoId}/tags - Add tag to photo
     * Story 3.3: Photo Tagging
     */
    @PostMapping("/photos/{photoId}/tags")
    public Mono<ResponseEntity<TagDTO>> addTag(
        @PathVariable UUID photoId,
        @RequestBody TagRequest request,
        @CurrentUser UserPrincipal currentUser
    ) {
        AddTagToPhotoCommand command = new AddTagToPhotoCommand(
            photoId,
            currentUser.userId(),
            request.tagName()
        );

        return addTagHandler.handle(command)
            .map(ResponseEntity::ok)
            .onErrorResume(AddTagToPhotoCommandHandler.MaxTagsExceededException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build())
            )
            .onErrorResume(AddTagToPhotoCommandHandler.PhotoNotFoundException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build())
            )
            .onErrorResume(AddTagToPhotoCommandHandler.UnauthorizedException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build())
            );
    }

    /**
     * DELETE /api/photos/{photoId}/tags/{tagId} - Remove tag from photo
     */
    @DeleteMapping("/photos/{photoId}/tags/{tagId}")
    public Mono<ResponseEntity<Void>> removeTag(
        @PathVariable UUID photoId,
        @PathVariable UUID tagId,
        @CurrentUser UserPrincipal currentUser
    ) {
        RemoveTagFromPhotoCommand command = new RemoveTagFromPhotoCommand(
            photoId,
            tagId,
            currentUser.userId()
        );

        return removeTagHandler.handle(command)
            .then(Mono.just(ResponseEntity.ok().<Void>build()))
            .onErrorResume(RemoveTagFromPhotoCommandHandler.PhotoNotFoundException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build())
            )
            .onErrorResume(RemoveTagFromPhotoCommandHandler.UnauthorizedException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build())
            );
    }

    /**
     * GET /api/tags - Get user's tags
     */
    @GetMapping("/tags")
    public Mono<ResponseEntity<List<TagDTO>>> getTags(
        @CurrentUser UserPrincipal currentUser
    ) {
        GetTagsForUserQuery query = new GetTagsForUserQuery(currentUser.userId());

        return getTagsHandler.handle(query)
            .collectList()
            .map(ResponseEntity::ok);
    }

    /**
     * GET /api/photos?tags=tag1,tag2 - Filter photos by tags
     * Story 3.4: Tag Filter (TODO: Implement in Story 3.4)
     */
    @GetMapping("/photos/by-tags")
    public Mono<ResponseEntity<?>> getPhotosByTags(
        @RequestParam String tags,
        @CurrentUser UserPrincipal currentUser
    ) {
        // Placeholder for Story 3.4
        return Mono.just(ResponseEntity.ok()
            .body(java.util.List.of()));
    }

    public record TagRequest(String tagName) {}
}
