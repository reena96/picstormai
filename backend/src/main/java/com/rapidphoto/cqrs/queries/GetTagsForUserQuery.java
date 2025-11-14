package com.rapidphoto.cqrs.queries;

import java.util.UUID;

/**
 * Query to get all tags for a user.
 */
public record GetTagsForUserQuery(
    UUID userId
) {}
