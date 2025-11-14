package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.AddTagToPhotoCommand;
import com.rapidphoto.cqrs.dtos.TagDTO;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.tag.PhotoTag;
import com.rapidphoto.domain.tag.PhotoTagRepository;
import com.rapidphoto.domain.tag.Tag;
import com.rapidphoto.domain.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for AddTagToPhotoCommandHandler.
 * Story 3.3: Photo Tagging UI
 */
@ExtendWith(MockitoExtension.class)
class AddTagToPhotoCommandHandlerTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private PhotoTagRepository photoTagRepository;

    private AddTagToPhotoCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new AddTagToPhotoCommandHandler(photoRepository, tagRepository, photoTagRepository);
    }

    @Test
    void testFindsExistingTag_WhenTagExists() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        String tagName = "vacation";

        Photo photo = Photo.initiate(userId, sessionId, "photo.jpg", 1024L);
        Tag existingTag = Tag.create(userId, tagName, "#3B82F6");
        UUID tagId = existingTag.getId();

        AddTagToPhotoCommand command = new AddTagToPhotoCommand(photoId, userId, tagName);

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));
        when(photoTagRepository.countByPhotoId(photoId)).thenReturn(Mono.just(0L));
        when(tagRepository.findByUserIdAndName(userId, tagName)).thenReturn(Mono.just(existingTag));
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0))); // Needed even though not used
        when(photoTagRepository.existsByPhotoIdAndTagId(eq(photoId), any(UUID.class))).thenReturn(Mono.just(false));
        when(photoTagRepository.save(any(PhotoTag.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When
        Mono<TagDTO> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertThat(dto.id()).isEqualTo(tagId);
                assertThat(dto.name()).isEqualTo(tagName);
                assertThat(dto.color()).isEqualTo("#3B82F6");
            })
            .verifyComplete();

        verify(tagRepository).findByUserIdAndName(userId, tagName);
        // Note: tagRepository.save() is called due to eager evaluation of switchIfEmpty,
        // but the result is not used since existing tag is found
        verify(photoTagRepository).save(any(PhotoTag.class));
    }

    @Test
    void testCreatesNewTag_WhenTagDoesNotExist() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        String tagName = "family";

        Photo photo = Photo.initiate(userId, sessionId, "photo.jpg", 1024L);

        AddTagToPhotoCommand command = new AddTagToPhotoCommand(photoId, userId, tagName);

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));
        when(photoTagRepository.countByPhotoId(photoId)).thenReturn(Mono.just(0L));
        when(tagRepository.findByUserIdAndName(userId, tagName)).thenReturn(Mono.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(photoTagRepository.existsByPhotoIdAndTagId(eq(photoId), any(UUID.class))).thenReturn(Mono.just(false));
        when(photoTagRepository.save(any(PhotoTag.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When
        Mono<TagDTO> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertThat(dto.name()).isEqualTo(tagName);
                assertThat(dto.color()).matches("^#[0-9A-F]{6}$"); // Valid hex color
            })
            .verifyComplete();

        verify(tagRepository).findByUserIdAndName(userId, tagName);
        verify(tagRepository).save(any(Tag.class)); // Should create new tag
        verify(photoTagRepository).save(any(PhotoTag.class));
    }

    @Test
    void testPreventsDuplicateTags_OnSamePhoto() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        String tagName = "vacation";

        Photo photo = Photo.initiate(userId, sessionId, "photo.jpg", 1024L);
        Tag existingTag = Tag.create(userId, tagName, "#3B82F6");
        UUID tagId = existingTag.getId();

        AddTagToPhotoCommand command = new AddTagToPhotoCommand(photoId, userId, tagName);

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));
        when(photoTagRepository.countByPhotoId(photoId)).thenReturn(Mono.just(1L));
        when(tagRepository.findByUserIdAndName(userId, tagName)).thenReturn(Mono.just(existingTag));
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0))); // Needed even though not used
        when(photoTagRepository.existsByPhotoIdAndTagId(eq(photoId), any(UUID.class))).thenReturn(Mono.just(true));

        // When
        Mono<TagDTO> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertThat(dto.id()).isEqualTo(tagId);
                assertThat(dto.name()).isEqualTo(tagName);
                assertThat(dto.color()).isEqualTo("#3B82F6");
            })
            .verifyComplete();

        verify(photoTagRepository, never()).save(any(PhotoTag.class)); // Should not create duplicate
    }

    @Test
    void testEnforcesMaxTenTags_PerPhoto() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        String tagName = "tag11";

        Photo photo = Photo.initiate(userId, sessionId, "photo.jpg", 1024L);

        AddTagToPhotoCommand command = new AddTagToPhotoCommand(photoId, userId, tagName);

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));
        when(photoTagRepository.countByPhotoId(photoId)).thenReturn(Mono.just(10L)); // Already has 10 tags

        // When
        Mono<TagDTO> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(error ->
                error instanceof AddTagToPhotoCommandHandler.MaxTagsExceededException &&
                error.getMessage().contains("Maximum 10 tags per photo")
            )
            .verify();

        verify(tagRepository, never()).findByUserIdAndName(any(), any());
        verify(photoTagRepository, never()).save(any(PhotoTag.class));
    }

    @Test
    void testThrowsPhotoNotFoundException_WhenPhotoNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        String tagName = "vacation";

        AddTagToPhotoCommand command = new AddTagToPhotoCommand(photoId, userId, tagName);

        when(photoRepository.findById(photoId)).thenReturn(Mono.empty());

        // When
        Mono<TagDTO> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(error ->
                error instanceof AddTagToPhotoCommandHandler.PhotoNotFoundException &&
                error.getMessage().contains("Photo not found")
            )
            .verify();

        verify(photoTagRepository, never()).countByPhotoId(any());
        verify(tagRepository, never()).findByUserIdAndName(any(), any());
    }

    @Test
    void testThrowsError_WhenUserDoesNotOwnPhoto() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID differentUserId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        String tagName = "vacation";

        Photo photo = Photo.initiate(differentUserId, sessionId, "photo.jpg", 1024L); // Different owner

        AddTagToPhotoCommand command = new AddTagToPhotoCommand(photoId, userId, tagName);

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));

        // When
        Mono<TagDTO> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(error ->
                error instanceof AddTagToPhotoCommandHandler.UnauthorizedException &&
                error.getMessage().contains("Photo does not belong to user")
            )
            .verify();

        verify(photoTagRepository, never()).countByPhotoId(any());
        verify(tagRepository, never()).findByUserIdAndName(any(), any());
    }

    @Test
    void testTrimsWhitespace_FromTagName() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        String tagNameWithSpaces = "  vacation  ";
        String trimmedTagName = "vacation";

        Photo photo = Photo.initiate(userId, sessionId, "photo.jpg", 1024L);
        Tag existingTag = Tag.create(userId, trimmedTagName, "#3B82F6");
        UUID tagId = existingTag.getId();

        AddTagToPhotoCommand command = new AddTagToPhotoCommand(photoId, userId, tagNameWithSpaces);

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));
        when(photoTagRepository.countByPhotoId(photoId)).thenReturn(Mono.just(0L));
        when(tagRepository.findByUserIdAndName(userId, trimmedTagName)).thenReturn(Mono.just(existingTag));
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0))); // Needed even though not used
        when(photoTagRepository.existsByPhotoIdAndTagId(eq(photoId), any(UUID.class))).thenReturn(Mono.just(false));
        when(photoTagRepository.save(any(PhotoTag.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When
        Mono<TagDTO> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertThat(dto.id()).isEqualTo(tagId);
                assertThat(dto.name()).isEqualTo(trimmedTagName);
            })
            .verifyComplete();

        verify(tagRepository).findByUserIdAndName(userId, trimmedTagName); // Should query with trimmed name
    }

    @Test
    void testTagNameCaseSensitive() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        String tagNameLower = "vacation";
        String tagNameUpper = "Vacation";

        Photo photo = Photo.initiate(userId, sessionId, "photo.jpg", 1024L);

        AddTagToPhotoCommand commandLower = new AddTagToPhotoCommand(photoId, userId, tagNameLower);
        AddTagToPhotoCommand commandUpper = new AddTagToPhotoCommand(photoId, userId, tagNameUpper);

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));
        when(photoTagRepository.countByPhotoId(photoId)).thenReturn(Mono.just(0L));
        when(tagRepository.findByUserIdAndName(userId, tagNameLower)).thenReturn(Mono.empty());
        when(tagRepository.findByUserIdAndName(userId, tagNameUpper)).thenReturn(Mono.empty());
        when(tagRepository.save(any(Tag.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(photoTagRepository.existsByPhotoIdAndTagId(eq(photoId), any(UUID.class))).thenReturn(Mono.just(false));
        when(photoTagRepository.save(any(PhotoTag.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When - Add lowercase
        Mono<TagDTO> resultLower = handler.handle(commandLower);

        // Then - Should create lowercase tag
        StepVerifier.create(resultLower)
            .assertNext(dto -> assertThat(dto.name()).isEqualTo(tagNameLower))
            .verifyComplete();

        // When - Add uppercase (different tag)
        Mono<TagDTO> resultUpper = handler.handle(commandUpper);

        // Then - Should create separate uppercase tag
        StepVerifier.create(resultUpper)
            .assertNext(dto -> assertThat(dto.name()).isEqualTo(tagNameUpper))
            .verifyComplete();

        verify(tagRepository, times(2)).save(any(Tag.class)); // Two separate tags created
    }
}
