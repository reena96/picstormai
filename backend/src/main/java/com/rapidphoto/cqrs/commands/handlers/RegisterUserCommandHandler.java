package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.RegisterUserCommand;
import com.rapidphoto.domain.events.UserRegisteredEvent;
import com.rapidphoto.domain.shared.Email;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserPreferences;
import com.rapidphoto.domain.user.UserPreferencesRepository;
import com.rapidphoto.domain.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for RegisterUserCommand.
 * Creates a new user with default preferences and publishes UserRegisteredEvent.
 */
@Service
public class RegisterUserCommandHandler {

    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RegisterUserCommandHandler(
        UserRepository userRepository,
        UserPreferencesRepository userPreferencesRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Mono<UUID> handle(RegisterUserCommand command) {
        return userRepository.existsByEmail(command.email())
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new IllegalArgumentException("Email already registered: " + command.email()));
                }

                // Create user
                Email email = Email.of(command.email());
                User user = User.create(email, command.password(), command.displayName());

                // Save user first
                return userRepository.save(user)
                    .flatMap(savedUser -> {
                        // Create default preferences
                        UserPreferences preferences = UserPreferences.createDefault(savedUser.getId());

                        // Save preferences
                        return userPreferencesRepository.save(preferences)
                            .map(savedPreferences -> savedUser);
                    })
                    .doOnSuccess(savedUser -> {
                        // Publish domain event
                        eventPublisher.publishEvent(new UserRegisteredEvent(
                            savedUser.getId(),
                            savedUser.getEmail().getValue()
                        ));
                    })
                    .map(User::getId);
            });
    }
}
