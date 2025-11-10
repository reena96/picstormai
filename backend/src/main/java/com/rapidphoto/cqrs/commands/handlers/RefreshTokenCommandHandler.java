package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.config.JwtConfig;
import com.rapidphoto.cqrs.commands.RefreshTokenCommand;
import com.rapidphoto.cqrs.dtos.RefreshTokenResponse;
import com.rapidphoto.domain.refreshtoken.RefreshToken;
import com.rapidphoto.domain.refreshtoken.RefreshTokenRepository;
import com.rapidphoto.domain.refreshtoken.events.RefreshTokenIssuedEvent;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserRepository;
import com.rapidphoto.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for RefreshTokenCommand.
 * Validates refresh token, rotates it (delete old, create new),
 * and generates new access token.
 */
@Service
public class RefreshTokenCommandHandler {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final JwtConfig jwtConfig;
    private final ApplicationEventPublisher eventPublisher;

    public RefreshTokenCommandHandler(
        RefreshTokenRepository refreshTokenRepository,
        UserRepository userRepository,
        JwtUtil jwtUtil,
        JwtConfig jwtConfig,
        ApplicationEventPublisher eventPublisher
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.jwtConfig = jwtConfig;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(rollbackFor = Exception.class)
    public Mono<RefreshTokenResponse> handle(RefreshTokenCommand command) {
        // Extract user ID from JWT refresh token
        UUID userId;
        try {
            userId = jwtUtil.getUserIdFromToken(command.refreshToken());
        } catch (JwtException e) {
            return Mono.error(new IllegalArgumentException("Invalid or expired refresh token"));
        }

        // Find all refresh tokens for user and validate
        return refreshTokenRepository.findByUserId(userId)
            .collectList()
            .flatMap(tokens -> {
                // Find matching token (verify hash)
                RefreshToken matchingToken = tokens.stream()
                    .filter(token -> token.isValid(command.refreshToken()))
                    .findFirst()
                    .orElse(null);

                if (matchingToken == null) {
                    return Mono.error(new IllegalArgumentException("Invalid or expired refresh token"));
                }

                // Get user details for new access token
                return userRepository.findById(userId)
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("User not found")))
                    .flatMap(user -> {
                        // Generate new access token
                        String newAccessToken = jwtUtil.generateAccessToken(
                            user.getId(),
                            user.getEmail().getValue()
                        );

                        // Generate new refresh token (rotation)
                        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId());

                        // Create new refresh token entity
                        RefreshToken newRefreshTokenEntity = RefreshToken.create(
                            user.getId(),
                            newRefreshToken,
                            jwtConfig.getRefreshTokenExpirationDays()
                        );

                        // Delete ALL refresh tokens for this user (ensure only one active token)
                        return refreshTokenRepository.deleteByUserId(user.getId())
                            .then(refreshTokenRepository.save(newRefreshTokenEntity))
                            .doOnSuccess(savedToken -> {
                                // Publish domain event
                                eventPublisher.publishEvent(new RefreshTokenIssuedEvent(
                                    savedToken.getId(),
                                    savedToken.getUserId(),
                                    savedToken.getExpiresAt(),
                                    savedToken.getCreatedAt()
                                ));
                            })
                            .map(savedToken -> RefreshTokenResponse.of(
                                newAccessToken,
                                newRefreshToken,
                                jwtConfig.getAccessTokenExpirationMinutes()
                            ));
                    });
            });
    }
}
