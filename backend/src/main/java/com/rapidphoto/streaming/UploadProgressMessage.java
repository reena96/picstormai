package com.rapidphoto.streaming;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base interface for upload progress messages sent via SSE.
 * Story 2.6: Real-Time Streaming Infrastructure Setup
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = PhotoUploadedMessage.class, name = "PHOTO_UPLOADED"),
    @JsonSubTypes.Type(value = PhotoFailedMessage.class, name = "PHOTO_FAILED"),
    @JsonSubTypes.Type(value = SessionCompletedMessage.class, name = "SESSION_COMPLETED")
})
public sealed interface UploadProgressMessage
    permits PhotoUploadedMessage, PhotoFailedMessage, SessionCompletedMessage {

    /**
     * The session ID this message belongs to
     */
    String sessionId();

    /**
     * The message type for routing on client
     */
    String type();
}
