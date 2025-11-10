package com.rapidphoto.cqrs.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Response DTO for user registration.
 * Contains user ID and message to check email.
 */
public record RegisterResponse(
    @JsonProperty("user_id")
    UUID userId,

    @JsonProperty("message")
    String message
) {
    public RegisterResponse {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (message == null || message.isEmpty()) {
            message = "Registration successful. Please check your email to verify your account.";
        }
    }

    /**
     * Factory method with default message.
     */
    public static RegisterResponse of(UUID userId) {
        return new RegisterResponse(
            userId,
            "Registration successful. Please check your email to verify your account."
        );
    }
}
