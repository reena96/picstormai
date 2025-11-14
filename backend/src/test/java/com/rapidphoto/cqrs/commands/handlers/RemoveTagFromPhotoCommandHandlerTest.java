package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.RemoveTagFromPhotoCommand;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.tag.PhotoTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for RemoveTagFromPhotoCommandHandler.
 * Story 3.3: Photo Tagging UI
 */
@ExtendWith(MockitoExtension.class)
class RemoveTagFromPhotoCommandHandlerTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private PhotoTagRepository photoTagRepository;

    private RemoveTagFromPhotoCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RemoveTagFromPhotoCommandHandler(photoRepository, photoTagRepository);
    }

    @Test
    void testRemovesTag_Successfully() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();

        Photo photo = Photo.initiate(userId, sessionId, "photo.jpg", 1024L);

        RemoveTagFromPhotoCommand command = new RemoveTagFromPhotoCommand(photoId, tagId, userId);

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));
        when(photoTagRepository.deleteByPhotoIdAndTagId(photoId, tagId)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        verify(photoRepository).findById(photoId);
        verify(photoTagRepository).deleteByPhotoIdAndTagId(photoId, tagId);
    }

    @Test
    void testTagEntityRemains_AfterRemoval() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();

        Photo photo = Photo.initiate(userId, sessionId, "photo.jpg", 1024L);

        RemoveTagFromPhotoCommand command = new RemoveTagFromPhotoCommand(photoId, tagId, userId);

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));
        when(photoTagRepository.deleteByPhotoIdAndTagId(photoId, tagId)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .verifyComplete();

        // Tag entity should remain - only photo_tag relationship is deleted
        verify(photoTagRepository).deleteByPhotoIdAndTagId(photoId, tagId);
        // TagRepository should not be involved in this operation
    }

    @Test
    void testThrowsPhotoNotFoundException_WhenPhotoNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();

        RemoveTagFromPhotoCommand command = new RemoveTagFromPhotoCommand(photoId, tagId, userId);

        when(photoRepository.findById(photoId)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(error ->
                error instanceof RemoveTagFromPhotoCommandHandler.PhotoNotFoundException &&
                error.getMessage().contains("Photo not found")
            )
            .verify();

        verify(photoRepository).findById(photoId);
        verify(photoTagRepository, never()).deleteByPhotoIdAndTagId(any(), any());
    }

    @Test
    void testThrowsError_WhenUserDoesNotOwnPhoto() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();

        Photo photo = Photo.initiate(differentUserId, sessionId, "photo.jpg", 1024L); // Different owner

        RemoveTagFromPhotoCommand command = new RemoveTagFromPhotoCommand(photoId, tagId, userId);

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));

        // When
        Mono<Void> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(error ->
                error instanceof RemoveTagFromPhotoCommandHandler.UnauthorizedException &&
                error.getMessage().contains("Photo does not belong to user")
            )
            .verify();

        verify(photoRepository).findById(photoId);
        verify(photoTagRepository, never()).deleteByPhotoIdAndTagId(any(), any());
    }
}
