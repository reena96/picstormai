package com.rapidphoto.cqrs.dtos;

import com.rapidphoto.domain.user.User;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for User data.
 * Never expose domain entities directly - always use DTOs.
 */
public record UserDTO(
    UUID id,
    String email,
    String displayName,
    boolean emailVerified,
    boolean hasSeenOnboarding,
    Instant createdAt,
    Instant lastLoginAt
) {
    /**
     * Create DTO from domain entity.
     */
    public static UserDTO fromDomain(User user) {
        return new UserDTO(
            user.getId(),
            user.getEmail().getValue(),
            user.getDisplayName(),
            user.isEmailVerified(),
            user.isHasSeenOnboarding(),
            user.getCreatedAt(),
            user.getLastLoginAt()
        );
    }
}
