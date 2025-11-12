package com.rapidphoto.streaming;

/**
 * Message indicating a photo was successfully uploaded.
 * Story 2.6: Real-Time Streaming Infrastructure Setup
 */
public record PhotoUploadedMessage(
    String sessionId,
    String photoId,
    int uploadedCount,
    int totalCount,
    int progressPercent
) implements UploadProgressMessage {

    @Override
    public String type() {
        return "PHOTO_UPLOADED";
    }
}
