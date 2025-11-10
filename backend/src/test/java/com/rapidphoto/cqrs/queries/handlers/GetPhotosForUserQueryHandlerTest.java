package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.PhotoDTO;
import com.rapidphoto.cqrs.queries.GetPhotosForUserQuery;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPhotosForUserQueryHandlerTest {

    @Mock
    private PhotoRepository photoRepository;

    private GetPhotosForUserQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetPhotosForUserQueryHandler(photoRepository);
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

        // When
        Flux<PhotoDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertThat(dto.filename()).isEqualTo("photo1.jpg");
                assertThat(dto.userId()).isEqualTo(userId);
            })
            .assertNext(dto -> {
                assertThat(dto.filename()).isEqualTo("photo2.jpg");
                assertThat(dto.userId()).isEqualTo(userId);
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
        Flux<PhotoDTO> result = handler.handle(query);

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

        // When
        Flux<PhotoDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> assertThat(dto.filename()).isEqualTo("photo3.jpg"))
            .verifyComplete();
    }
}
