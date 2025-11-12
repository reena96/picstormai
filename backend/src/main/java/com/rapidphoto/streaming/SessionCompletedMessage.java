package com.rapidphoto.streaming;

/**
 * Message indicating all photos in a session have finished uploading.
 * Story 2.6: Real-Time Streaming Infrastructure Setup
 */
public record SessionCompletedMessage(
    String sessionId,
    int successCount,
    int failedCount,
    int totalCount
) implements UploadProgressMessage {

    @Override
    public String type() {
        return "SESSION_COMPLETED";
    }
}
