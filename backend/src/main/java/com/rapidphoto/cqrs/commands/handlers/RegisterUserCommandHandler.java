package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.RegisterUserCommand;
import com.rapidphoto.domain.events.UserRegisteredEvent;
import com.rapidphoto.domain.shared.Email;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserPreferences;
import com.rapidphoto.domain.user.UserPreferencesRepository;
import com.rapidphoto.domain.user.UserRepository;
import com.rapidphoto.domain.verification.EmailVerificationToken;
import com.rapidphoto.domain.verification.EmailVerificationTokenRepository;
import com.rapidphoto.email.EmailService;
import com.rapidphoto.security.PasswordValidator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for RegisterUserCommand.
 * Creates a new user with default preferences, generates email verification token,
 * sends verification email, and publishes UserRegisteredEvent.
 */
@Service
public class RegisterUserCommandHandler {

    private final UserRepository userRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;
    private final ApplicationEventPublisher eventPublisher;

    public RegisterUserCommandHandler(
        UserRepository userRepository,
        UserPreferencesRepository userPreferencesRepository,
        EmailVerificationTokenRepository verificationTokenRepository,
        EmailService emailService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.emailService = emailService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public Mono<UUID> handle(RegisterUserCommand command) {
        return Mono.defer(() -> {
            // Validate password strength
            PasswordValidator.validateOrThrow(command.password());
            return userRepository.existsByEmail(command.email());
        })
            .flatMap(exists -> {
                if (exists) {
                    return Mono.error(new IllegalArgumentException("Email already registered"));
                }

                // Create user (email not verified yet)
                Email email = Email.of(command.email());
                User user = User.create(email, command.password(), command.displayName());

                // Generate email verification token
                EmailVerificationToken verificationToken = EmailVerificationToken.create(user.getId());

                // Save user, preferences, and verification token
                return userRepository.save(user)
                    .flatMap(savedUser -> {
                        // Create default preferences
                        UserPreferences preferences = UserPreferences.createDefault(savedUser.getId());

                        // Save preferences and verification token in parallel
                        return Mono.zip(
                            userPreferencesRepository.save(preferences),
                            verificationTokenRepository.save(verificationToken)
                        ).map(tuple -> savedUser);
                    })
                    .flatMap(savedUser -> {
                        // Send verification email
                        return emailService.sendVerificationEmail(
                            savedUser.getEmail().getValue(),
                            savedUser.getDisplayName(),
                            verificationToken.getToken()
                        ).thenReturn(savedUser);
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
