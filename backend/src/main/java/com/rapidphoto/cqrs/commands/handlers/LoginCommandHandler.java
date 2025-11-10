package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.config.JwtConfig;
import com.rapidphoto.cqrs.commands.LoginCommand;
import com.rapidphoto.cqrs.dtos.LoginResponse;
import com.rapidphoto.domain.refreshtoken.RefreshToken;
import com.rapidphoto.domain.refreshtoken.RefreshTokenRepository;
import com.rapidphoto.domain.refreshtoken.events.RefreshTokenIssuedEvent;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserRepository;
import com.rapidphoto.security.JwtUtil;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Handler for LoginCommand.
 * Authenticates user, generates JWT tokens (access + refresh),
 * and records login timestamp.
 */
@Service
public class LoginCommandHandler {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;
    private final ApplicationEventPublisher eventPublisher;

    public LoginCommandHandler(
        UserRepository userRepository,
        RefreshTokenRepository refreshTokenRepository,
        JwtUtil jwtUtil,
        JwtConfig jwtConfig,
        ApplicationEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.jwtConfig = jwtConfig;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public Mono<LoginResponse> handle(LoginCommand command) {
        return userRepository.findByEmail(command.email())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid email or password")))
            .flatMap(user -> {
                // Verify password
                if (!user.checkPassword(command.password())) {
                    return Mono.error(new IllegalArgumentException("Invalid email or password"));
                }

                // Check if email is verified
                if (!user.isEmailVerified()) {
                    return Mono.error(new IllegalArgumentException("Please verify your email before logging in"));
                }

                // Record login timestamp
                user.recordLogin();

                // Generate JWT access token (15 minutes)
                String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail().getValue());

                // Generate JWT refresh token (30 days)
                String refreshToken = jwtUtil.generateRefreshToken(user.getId());

                // Create refresh token entity (stores BCrypt hash)
                RefreshToken refreshTokenEntity = RefreshToken.create(
                    user.getId(),
                    refreshToken,
                    jwtConfig.getRefreshTokenExpirationDays()
                );

                // Delete old refresh tokens and save new one (ensure only one active token per user)
                return userRepository.save(user)
                    .then(refreshTokenRepository.deleteByUserId(user.getId()))
                    .then(refreshTokenRepository.save(refreshTokenEntity))
                    .doOnSuccess(savedToken -> {
                        // Publish domain event
                        eventPublisher.publishEvent(new RefreshTokenIssuedEvent(
                            savedToken.getId(),
                            savedToken.getUserId(),
                            savedToken.getExpiresAt(),
                            savedToken.getCreatedAt()
                        ));
                    })
                    .map(savedToken -> LoginResponse.of(
                        accessToken,
                        refreshToken,
                        jwtConfig.getAccessTokenExpirationMinutes()
                    ));
            });
    }
}
