package com.rapidphoto.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

/**
 * Test controller for verifying JWT authentication.
 * Protected endpoint that requires valid JWT token.
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * Protected endpoint - requires valid JWT token.
     * Returns user ID from authentication context.
     */
    @GetMapping("/protected")
    public Mono<Map<String, Object>> protectedEndpoint(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();

        return Mono.just(Map.of(
            "message", "You are authenticated!",
            "userId", userId.toString(),
            "authorities", authentication.getAuthorities()
        ));
    }
}
