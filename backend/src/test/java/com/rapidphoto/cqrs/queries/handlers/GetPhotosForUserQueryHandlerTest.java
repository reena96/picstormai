package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.PhotoWithTagsDTO;
import com.rapidphoto.cqrs.queries.GetPhotosForUserQuery;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPhotosForUserQueryHandlerTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private TagRepository tagRepository;

    private GetPhotosForUserQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetPhotosForUserQueryHandler(photoRepository, tagRepository);
    }

    @Test
    void shouldReturnPhotoDTOsWithPagination() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        Photo photo1 = Photo.initiate(userId, sessionId, "photo1.jpg", 1024L);
        Photo photo2 = Photo.initiate(userId, sessionId, "photo2.jpg", 2048L);
        Photo photo3 = Photo.initiate(userId, sessionId, "photo3.jpg", 3072L);

        GetPhotosForUserQuery query = new GetPhotosForUserQuery(userId, 0, 2);

        when(photoRepository.findByUserId(userId)).thenReturn(Flux.just(photo1, photo2, photo3));
        when(tagRepository.findByPhotoIds(any())).thenReturn(Flux.empty());

        // When
        Flux<PhotoWithTagsDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertThat(dto.filename()).isEqualTo("photo1.jpg");
                assertThat(dto.tags()).isEmpty();
            })
            .assertNext(dto -> {
                assertThat(dto.filename()).isEqualTo("photo2.jpg");
                assertThat(dto.tags()).isEmpty();
            })
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenNoPhotos() {
        // Given
        UUID userId = UUID.randomUUID();
        GetPhotosForUserQuery query = new GetPhotosForUserQuery(userId, 0, 10);

        when(photoRepository.findByUserId(userId)).thenReturn(Flux.empty());

        // When
        Flux<PhotoWithTagsDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void shouldApplyPaginationCorrectly() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        Photo photo1 = Photo.initiate(userId, sessionId, "photo1.jpg", 1024L);
        Photo photo2 = Photo.initiate(userId, sessionId, "photo2.jpg", 2048L);
        Photo photo3 = Photo.initiate(userId, sessionId, "photo3.jpg", 3072L);

        // Page 1 (skip 2, take 1)
        GetPhotosForUserQuery query = new GetPhotosForUserQuery(userId, 1, 2);

        when(photoRepository.findByUserId(userId)).thenReturn(Flux.just(photo1, photo2, photo3));
        when(tagRepository.findByPhotoIds(any())).thenReturn(Flux.empty());

        // When
        Flux<PhotoWithTagsDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> assertThat(dto.filename()).isEqualTo("photo3.jpg"))
            .verifyComplete();
    }

    @Test
    void shouldReturnPhotosWithSingleTag() {
        // Given - Story 3.4: Tag filtering with single tag
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();

        Photo photo1 = Photo.initiate(userId, sessionId, "photo1.jpg", 1024L);
        Photo photo2 = Photo.initiate(userId, sessionId, "photo2.jpg", 2048L);

        List<UUID> tagIds = List.of(tagId);
        GetPhotosForUserQuery query = new GetPhotosForUserQuery(userId, 0, 10, tagIds);

        when(photoRepository.findByUserIdAndAllTags(userId, tagIds, 1))
            .thenReturn(Flux.just(photo1, photo2));
        when(tagRepository.findByPhotoIds(any())).thenReturn(Flux.empty());

        // When
        Flux<PhotoWithTagsDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> assertThat(dto.filename()).isEqualTo("photo1.jpg"))
            .assertNext(dto -> assertThat(dto.filename()).isEqualTo("photo2.jpg"))
            .verifyComplete();

        verify(photoRepository).findByUserIdAndAllTags(userId, tagIds, 1);
    }

    @Test
    void shouldReturnPhotosWithMultipleTags_AND_Logic() {
        // Given - Story 3.4: Tag filtering with multiple tags (AND logic)
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID tagId1 = UUID.randomUUID();
        UUID tagId2 = UUID.randomUUID();

        Photo photo1 = Photo.initiate(userId, sessionId, "photo1.jpg", 1024L);

        List<UUID> tagIds = List.of(tagId1, tagId2);
        GetPhotosForUserQuery query = new GetPhotosForUserQuery(userId, 0, 10, tagIds);

        // Only photo1 has BOTH tags
        when(photoRepository.findByUserIdAndAllTags(userId, tagIds, 2))
            .thenReturn(Flux.just(photo1));
        when(tagRepository.findByPhotoIds(any())).thenReturn(Flux.empty());

        // When
        Flux<PhotoWithTagsDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> assertThat(dto.filename()).isEqualTo("photo1.jpg"))
            .verifyComplete();

        verify(photoRepository).findByUserIdAndAllTags(userId, tagIds, 2);
    }

    @Test
    void shouldReturnEmptyWhenNoPhotosMatchAllTags() {
        // Given - Story 3.4: No photos match all selected tags
        UUID userId = UUID.randomUUID();
        UUID tagId1 = UUID.randomUUID();
        UUID tagId2 = UUID.randomUUID();

        List<UUID> tagIds = List.of(tagId1, tagId2);
        GetPhotosForUserQuery query = new GetPhotosForUserQuery(userId, 0, 10, tagIds);

        when(photoRepository.findByUserIdAndAllTags(userId, tagIds, 2))
            .thenReturn(Flux.empty());

        // When
        Flux<PhotoWithTagsDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void shouldIgnoreTagFilter_WhenTagIdsNull() {
        // Given - Story 3.4: Null tagIds should use default behavior
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        Photo photo1 = Photo.initiate(userId, sessionId, "photo1.jpg", 1024L);

        GetPhotosForUserQuery query = new GetPhotosForUserQuery(userId, 0, 10, null);

        when(photoRepository.findByUserId(userId)).thenReturn(Flux.just(photo1));
        when(tagRepository.findByPhotoIds(any())).thenReturn(Flux.empty());

        // When
        Flux<PhotoWithTagsDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> assertThat(dto.filename()).isEqualTo("photo1.jpg"))
            .verifyComplete();

        verify(photoRepository).findByUserId(userId);
    }

    @Test
    void shouldIgnoreTagFilter_WhenTagIdsEmpty() {
        // Given - Story 3.4: Empty tagIds list should use default behavior
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        Photo photo1 = Photo.initiate(userId, sessionId, "photo1.jpg", 1024L);

        GetPhotosForUserQuery query = new GetPhotosForUserQuery(userId, 0, 10, List.of());

        when(photoRepository.findByUserId(userId)).thenReturn(Flux.just(photo1));
        when(tagRepository.findByPhotoIds(any())).thenReturn(Flux.empty());

        // When
        Flux<PhotoWithTagsDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> assertThat(dto.filename()).isEqualTo("photo1.jpg"))
            .verifyComplete();

        verify(photoRepository).findByUserId(userId);
    }

    @Test
    void shouldApplyPaginationWithTagFilter() {
        // Given - Story 3.4: Pagination works with tag filtering
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID tagId = UUID.randomUUID();

        Photo photo1 = Photo.initiate(userId, sessionId, "photo1.jpg", 1024L);
        Photo photo2 = Photo.initiate(userId, sessionId, "photo2.jpg", 2048L);
        Photo photo3 = Photo.initiate(userId, sessionId, "photo3.jpg", 3072L);

        List<UUID> tagIds = List.of(tagId);
        GetPhotosForUserQuery query = new GetPhotosForUserQuery(userId, 0, 2, tagIds);

        when(photoRepository.findByUserIdAndAllTags(userId, tagIds, 1))
            .thenReturn(Flux.just(photo1, photo2, photo3));
        when(tagRepository.findByPhotoIds(any())).thenReturn(Flux.empty());

        // When
        Flux<PhotoWithTagsDTO> result = handler.handle(query);

        // Then - Should return first 2 photos
        StepVerifier.create(result)
            .assertNext(dto -> assertThat(dto.filename()).isEqualTo("photo1.jpg"))
            .assertNext(dto -> assertThat(dto.filename()).isEqualTo("photo2.jpg"))
            .verifyComplete();
    }
}
