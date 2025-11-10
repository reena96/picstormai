package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.VerifyEmailCommand;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserRepository;
import com.rapidphoto.domain.verification.EmailVerificationToken;
import com.rapidphoto.domain.verification.EmailVerificationTokenRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for VerifyEmailCommand.
 * Validates verification token and marks user's email as verified.
 */
@Service
public class VerifyEmailCommandHandler {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final ApplicationEventPublisher eventPublisher;

    public VerifyEmailCommandHandler(
        UserRepository userRepository,
        EmailVerificationTokenRepository verificationTokenRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public Mono<UUID> handle(VerifyEmailCommand command) {
        // Find token in database
        return verificationTokenRepository.findByToken(command.token())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid or expired verification token")))
            .flatMap(token -> {
                // Check if token is expired
                if (token.isExpired()) {
                    return Mono.error(new IllegalArgumentException("Verification token expired"));
                }

                // Find user
                return userRepository.findById(token.getUserId())
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")))
                    .flatMap(user -> {
                        // Verify email (idempotent - won't fail if already verified)
                        user.verifyEmail();

                        // Save user and delete token
                        return userRepository.save(user)
                            .then(verificationTokenRepository.deleteByToken(command.token()))
                            .thenReturn(user.getId());
                    });
            });
    }
}
