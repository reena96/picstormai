package com.rapidphoto.cqrs.commands;

import java.util.UUID;

/**
 * Command to verify user's email address.
 */
public record VerifyEmailCommand(
    UUID userId
) {}
