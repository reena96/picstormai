package com.rapidphoto.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * WebSocket Publisher for Upload Progress
 * Story 2.7: Real-Time Progress Broadcasting
 */
@Service
public class UploadProgressPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public UploadProgressPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishPhotoUploaded(UUID sessionId, UUID photoId, int uploadedCount, int totalCount) {
        Map<String, Object> message = Map.of(
            "type", "PHOTO_UPLOADED",
            "sessionId", sessionId.toString(),
            "photoId", photoId.toString(),
            "uploadedCount", uploadedCount,
            "totalCount", totalCount,
            "progressPercent", (uploadedCount * 100.0) / totalCount
        );
        
        messagingTemplate.convertAndSend("/topic/upload-sessions/" + sessionId, message);
    }

    public void publishSessionCompleted(UUID sessionId) {
        Map<String, Object> message = Map.of(
            "type", "SESSION_COMPLETED",
            "sessionId", sessionId.toString()
        );
        
        messagingTemplate.convertAndSend("/topic/upload-sessions/" + sessionId, message);
    }
}
