package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.InitiatePhotoUploadCommand;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.infrastructure.S3Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Handler for InitiatePhotoUploadCommand.
 * Creates a new photo record and generates S3 pre-signed URL.
 * Story 2.3: S3 Pre-Signed URL Generation
 */
@Service
public class InitiatePhotoUploadCommandHandler {

    private final PhotoRepository photoRepository;
    private final S3Service s3Service;
    private final ApplicationEventPublisher eventPublisher;

    public InitiatePhotoUploadCommandHandler(
        PhotoRepository photoRepository,
        S3Service s3Service,
        ApplicationEventPublisher eventPublisher
    ) {
        this.photoRepository = photoRepository;
        this.s3Service = s3Service;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public Mono<Map<String, Object>> handle(InitiatePhotoUploadCommand command) {
        UUID photoId = UUID.randomUUID();

        // Create new photo
        Photo photo = Photo.initiate(
            command.userId(),
            command.sessionId(),
            command.filename(),
            command.fileSizeBytes()
        );

        // Generate pre-signed URL
        Map<String, Object> presignedUrlData = s3Service.generatePresignedUploadUrl(
            command.userId(),
            command.sessionId(),
            photoId,
            command.filename(),
            command.fileSizeBytes(),
            command.mimeType()
        );

        // Save photo
        return photoRepository.save(photo)
            .thenReturn(presignedUrlData);
    }
}
