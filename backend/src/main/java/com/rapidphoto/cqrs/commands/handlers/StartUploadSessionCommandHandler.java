package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.StartUploadSessionCommand;
import com.rapidphoto.domain.upload.UploadSession;
import com.rapidphoto.domain.upload.UploadSessionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for StartUploadSessionCommand.
 * Creates a new upload session.
 */
@Service
public class StartUploadSessionCommandHandler {

    private final UploadSessionRepository uploadSessionRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StartUploadSessionCommandHandler(
        UploadSessionRepository uploadSessionRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.uploadSessionRepository = uploadSessionRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public Mono<UUID> handle(StartUploadSessionCommand command) {
        // Create new session
        UploadSession session = UploadSession.start(command.userId());
        session.setTotalPhotos(command.totalPhotos());

        // Save session
        return uploadSessionRepository.save(session)
            .map(UploadSession::getId);
    }
}
