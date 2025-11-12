package com.rapidphoto.streaming;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Controller providing information about real-time streaming capabilities.
 * Story 2.6: Real-Time Streaming Infrastructure Setup
 */
@RestController
@RequestMapping("/api/realtime")
public class RealtimeInfoController {

    /**
     * Get information about real-time streaming endpoints and protocols.
     * Useful for client discovery and documentation.
     *
     * GET /api/realtime/info
     */
    @GetMapping("/info")
    public Mono<RealtimeInfo> getRealtimeInfo() {
        return Mono.just(new RealtimeInfo(
            "/api/upload/sessions/{sessionId}/stream",
            "Server-Sent Events (SSE)",
            List.of(
                "PHOTO_UPLOADED - Photo successfully uploaded to S3",
                "PHOTO_FAILED - Photo upload failed with error",
                "SESSION_COMPLETED - All photos in session finished uploading"
            ),
            "/api/upload/sessions/notifications/stream",
            List.of(
                "Authentication required: Include JWT token in Authorization header",
                "EventSource API automatically handles reconnection",
                "Heartbeat sent every 30 seconds to keep connection alive",
                "Connection timeout: 60 seconds of inactivity"
            )
        ));
    }
}

/**
 * Information about real-time streaming capabilities.
 */
record RealtimeInfo(
    String sessionStreamEndpoint,
    String protocol,
    List<String> messageTypes,
    String notificationStreamEndpoint,
    List<String> connectionInfo
) {}
