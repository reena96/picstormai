package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.MarkPhotoUploadedCommand;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.domain.photo.PhotoStatus;
import com.rapidphoto.domain.photo.S3Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Marks a photo as successfully uploaded after S3 upload completes.
 */
@Service
public class MarkPhotoUploadedCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(MarkPhotoUploadedCommandHandler.class);
    private final PhotoRepository photoRepository;
    private final String bucketName;

    public MarkPhotoUploadedCommandHandler(
        PhotoRepository photoRepository,
        @Value("${aws.s3.bucket-name}") String bucketName
    ) {
        this.photoRepository = photoRepository;
        this.bucketName = bucketName;
    }

    @Transactional
    public Mono<Void> handle(MarkPhotoUploadedCommand command) {
        log.info("Marking photo {} as uploaded with S3 key: {}", command.photoId(), command.s3Key());

        return photoRepository.findById(command.photoId())
            .flatMap(photo -> {
                // Verify ownership
                if (!photo.getUserId().equals(command.userId())) {
                    log.warn("User {} attempted to mark photo {} owned by {}",
                        command.userId(), command.photoId(), photo.getUserId());
                    return Mono.error(new IllegalArgumentException("Not authorized"));
                }

                // If still PENDING, start the upload first
                if (photo.getUploadStatus() == PhotoStatus.PENDING) {
                    photo.startUpload(S3Location.of(bucketName, command.s3Key()));
                }

                // Complete the upload
                photo.completeUpload(null);

                log.info("Photo {} marked as COMPLETED", command.photoId());
                return photoRepository.save(photo);
            })
            .then();
    }
}
