package com.rapidphoto.api;

import com.rapidphoto.cqrs.commands.LoginCommand;
import com.rapidphoto.cqrs.commands.RefreshTokenCommand;
import com.rapidphoto.cqrs.commands.handlers.LoginCommandHandler;
import com.rapidphoto.cqrs.commands.handlers.RefreshTokenCommandHandler;
import com.rapidphoto.cqrs.dtos.LoginResponse;
import com.rapidphoto.cqrs.dtos.RefreshTokenResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for authentication endpoints.
 * Handles login and token refresh operations.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginCommandHandler loginCommandHandler;
    private final RefreshTokenCommandHandler refreshTokenCommandHandler;

    public AuthController(
        LoginCommandHandler loginCommandHandler,
        RefreshTokenCommandHandler refreshTokenCommandHandler
    ) {
        this.loginCommandHandler = loginCommandHandler;
        this.refreshTokenCommandHandler = refreshTokenCommandHandler;
    }

    /**
     * POST /api/auth/login
     * Authenticates user and returns JWT tokens.
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(request.email(), request.password());

        return loginCommandHandler.handle(command)
            .map(response -> ResponseEntity.ok(response))
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null))
            );
    }

    /**
     * POST /api/auth/refresh
     * Refreshes access token using refresh token.
     */
    @PostMapping("/refresh")
    public Mono<ResponseEntity<RefreshTokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenCommand command = new RefreshTokenCommand(request.refreshToken());

        return refreshTokenCommandHandler.handle(command)
            .map(response -> ResponseEntity.ok(response))
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null))
            );
    }

    /**
     * Request DTO for login endpoint.
     */
    public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        String password
    ) {}

    /**
     * Request DTO for refresh token endpoint.
     */
    public record RefreshRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
    ) {}
}
