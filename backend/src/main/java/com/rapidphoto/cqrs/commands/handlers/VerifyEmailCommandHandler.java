package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.VerifyEmailCommand;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for VerifyEmailCommand.
 * Marks user's email as verified.
 */
@Service
public class VerifyEmailCommandHandler {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public VerifyEmailCommandHandler(
        UserRepository userRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Mono<UUID> handle(VerifyEmailCommand command) {
        return userRepository.findById(command.userId())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found: " + command.userId())))
            .flatMap(user -> {
                // Verify email
                user.verifyEmail();

                // Save updated user
                return userRepository.save(user)
                    .map(User::getId);
            });
    }
}
