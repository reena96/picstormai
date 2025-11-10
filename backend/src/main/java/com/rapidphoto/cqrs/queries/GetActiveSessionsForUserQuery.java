package com.rapidphoto.cqrs.queries;

import java.util.UUID;

/**
 * Query to get active upload sessions for a user.
 */
public record GetActiveSessionsForUserQuery(
    UUID userId
) {}
