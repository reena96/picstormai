package com.rapidphoto.cqrs.queries;

import java.util.UUID;

/**
 * Query to get user by ID.
 */
public record GetUserByIdQuery(
    UUID userId
) {}
