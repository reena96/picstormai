package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.CompletePhotoUploadCommand;
import com.rapidphoto.domain.events.PhotoUploadedEvent;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.upload.UploadSession;
import com.rapidphoto.domain.upload.UploadSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompletePhotoUploadCommandHandlerTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private UploadSessionRepository uploadSessionRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CompletePhotoUploadCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CompletePhotoUploadCommandHandler(photoRepository, uploadSessionRepository, eventPublisher);
    }

    @Test
    void shouldCompletePhotoUpload() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();

        Photo photo = Photo.initiate(userId, sessionId, "test.jpg", 1024L);
        UploadSession session = UploadSession.start(userId);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("width", 1920);
        metadata.put("height", 1080);

        CompletePhotoUploadCommand command = new CompletePhotoUploadCommand(
            photoId,
            "uploads/test.jpg",
            "version123",
            metadata
        );

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));
        when(photoRepository.save(any(Photo.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(uploadSessionRepository.findById(sessionId)).thenReturn(Mono.just(session));
        when(uploadSessionRepository.save(any(UploadSession.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When
        Mono<UUID> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .assertNext(resultId -> {
                assertThat(resultId).isEqualTo(photo.getId());
                verify(photoRepository).save(any(Photo.class));
                verify(uploadSessionRepository).save(any(UploadSession.class));
                verify(eventPublisher).publishEvent(any(PhotoUploadedEvent.class));
            })
            .verifyComplete();
    }

    @Test
    void shouldPublishPhotoUploadedEvent() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();

        Photo photo = Photo.initiate(userId, sessionId, "test.jpg", 1024L);
        UploadSession session = UploadSession.start(userId);

        CompletePhotoUploadCommand command = new CompletePhotoUploadCommand(
            photoId,
            "uploads/test.jpg",
            "version123",
            new HashMap<>()
        );

        when(photoRepository.findById(photoId)).thenReturn(Mono.just(photo));
        when(photoRepository.save(any(Photo.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(uploadSessionRepository.findById(sessionId)).thenReturn(Mono.just(session));
        when(uploadSessionRepository.save(any(UploadSession.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When
        handler.handle(command).block();

        // Then
        ArgumentCaptor<PhotoUploadedEvent> eventCaptor = ArgumentCaptor.forClass(PhotoUploadedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        PhotoUploadedEvent event = eventCaptor.getValue();
        assertThat(event.getPhotoId()).isEqualTo(photo.getId());
        assertThat(event.getUserId()).isEqualTo(userId);
        assertThat(event.getSessionId()).isEqualTo(sessionId);
        assertThat(event.getFilename()).isEqualTo("test.jpg");
    }

    @Test
    void shouldFailWhenPhotoNotFound() {
        // Given
        UUID photoId = UUID.randomUUID();
        CompletePhotoUploadCommand command = new CompletePhotoUploadCommand(
            photoId,
            "uploads/test.jpg",
            "version123",
            new HashMap<>()
        );

        when(photoRepository.findById(photoId)).thenReturn(Mono.empty());

        // When
        Mono<UUID> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Photo not found")
            )
            .verify();
    }
}
