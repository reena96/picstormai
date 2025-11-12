package com.rapidphoto.streaming;

/**
 * Message indicating a photo upload failed.
 * Story 2.6: Real-Time Streaming Infrastructure Setup
 */
public record PhotoFailedMessage(
    String sessionId,
    String photoId,
    String errorMessage,
    int failedCount,
    int totalCount
) implements UploadProgressMessage {

    @Override
    public String type() {
        return "PHOTO_FAILED";
    }
}
