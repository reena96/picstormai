package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.LoginCommand;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for LoginCommand.
 * Authenticates user and records login timestamp.
 */
@Service
public class LoginCommandHandler {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public LoginCommandHandler(
        UserRepository userRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Mono<UUID> handle(LoginCommand command) {
        return userRepository.findByEmail(command.email())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid email or password")))
            .flatMap(user -> {
                if (!user.checkPassword(command.password())) {
                    return Mono.error(new IllegalArgumentException("Invalid email or password"));
                }

                // Record login
                user.recordLogin();

                // Save updated user
                return userRepository.save(user)
                    .map(User::getId);
            });
    }
}
