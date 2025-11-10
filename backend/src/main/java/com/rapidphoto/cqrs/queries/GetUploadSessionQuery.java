package com.rapidphoto.cqrs.queries;

import java.util.UUID;

/**
 * Query to get upload session by ID.
 */
public record GetUploadSessionQuery(
    UUID sessionId
) {}
