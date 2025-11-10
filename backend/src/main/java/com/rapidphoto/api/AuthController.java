package com.rapidphoto.api;

import com.rapidphoto.cqrs.commands.LoginCommand;
import com.rapidphoto.cqrs.commands.RefreshTokenCommand;
import com.rapidphoto.cqrs.commands.RegisterUserCommand;
import com.rapidphoto.cqrs.commands.VerifyEmailCommand;
import com.rapidphoto.cqrs.commands.handlers.LoginCommandHandler;
import com.rapidphoto.cqrs.commands.handlers.RefreshTokenCommandHandler;
import com.rapidphoto.cqrs.commands.handlers.RegisterUserCommandHandler;
import com.rapidphoto.cqrs.commands.handlers.VerifyEmailCommandHandler;
import com.rapidphoto.cqrs.dtos.LoginResponse;
import com.rapidphoto.cqrs.dtos.RefreshTokenResponse;
import com.rapidphoto.cqrs.dtos.RegisterResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * REST controller for authentication endpoints.
 * Handles registration, login, email verification, and token refresh.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginCommandHandler loginCommandHandler;
    private final RefreshTokenCommandHandler refreshTokenCommandHandler;
    private final RegisterUserCommandHandler registerUserCommandHandler;
    private final VerifyEmailCommandHandler verifyEmailCommandHandler;

    public AuthController(
        LoginCommandHandler loginCommandHandler,
        RefreshTokenCommandHandler refreshTokenCommandHandler,
        RegisterUserCommandHandler registerUserCommandHandler,
        VerifyEmailCommandHandler verifyEmailCommandHandler
    ) {
        this.loginCommandHandler = loginCommandHandler;
        this.refreshTokenCommandHandler = refreshTokenCommandHandler;
        this.registerUserCommandHandler = registerUserCommandHandler;
        this.verifyEmailCommandHandler = verifyEmailCommandHandler;
    }

    /**
     * POST /api/auth/register
     * Registers new user and sends verification email.
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<RegisterResponse>> register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserCommand command = new RegisterUserCommand(
            request.email(),
            request.password(),
            request.displayName()
        );

        return registerUserCommandHandler.handle(command)
            .map(userId -> ResponseEntity.status(HttpStatus.CREATED)
                .body(RegisterResponse.of(userId)))
            .onErrorResume(IllegalArgumentException.class, e -> {
                if (e.getMessage().contains("already registered")) {
                    return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(null));
                } else if (e.getMessage().contains("Password")) {
                    return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(null));
                }
                return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null));
            });
    }

    /**
     * GET /api/auth/verify-email?token={token}
     * Verifies user email using verification token.
     */
    @GetMapping("/verify-email")
    public Mono<ResponseEntity<Map<String, String>>> verifyEmail(@RequestParam String token) {
        VerifyEmailCommand command = new VerifyEmailCommand(token);

        return verifyEmailCommandHandler.handle(command)
            .map(userId -> ResponseEntity.ok(Map.of(
                "message", "Email verified successfully! You can now log in."
            )))
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage())))
            );
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
     * Request DTO for registration endpoint.
     */
    public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Password is required")
        String password,

        @NotBlank(message = "Display name is required")
        String displayName
    ) {}

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
