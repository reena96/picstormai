package com.rapidphoto.cqrs.queries;

import java.util.UUID;

/**
 * Query to get user preferences.
 */
public record GetUserPreferencesQuery(
    UUID userId
) {}
