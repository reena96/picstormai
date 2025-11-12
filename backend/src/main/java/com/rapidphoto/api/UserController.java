package com.rapidphoto.api;

import com.rapidphoto.cqrs.commands.UpdateUserFlagCommand;
import com.rapidphoto.cqrs.commands.UpdateUserPreferencesCommand;
import com.rapidphoto.cqrs.commands.handlers.UpdateUserFlagCommandHandler;
import com.rapidphoto.cqrs.commands.handlers.UpdateUserPreferencesCommandHandler;
import com.rapidphoto.cqrs.dtos.UserDTO;
import com.rapidphoto.cqrs.dtos.UserPreferencesDTO;
import com.rapidphoto.cqrs.queries.GetUserByIdQuery;
import com.rapidphoto.cqrs.queries.GetUserPreferencesQuery;
import com.rapidphoto.cqrs.queries.handlers.GetUserByIdQueryHandler;
import com.rapidphoto.cqrs.queries.handlers.GetUserPreferencesQueryHandler;
import com.rapidphoto.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * REST controller for user-related endpoints.
 * Handles user profile, preferences, and flags.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UpdateUserPreferencesCommandHandler updatePreferencesHandler;
    private final GetUserPreferencesQueryHandler getPreferencesHandler;
    private final UpdateUserFlagCommandHandler updateUserFlagHandler;
    private final GetUserByIdQueryHandler getUserByIdHandler;

    public UserController(
        UpdateUserPreferencesCommandHandler updatePreferencesHandler,
        GetUserPreferencesQueryHandler getPreferencesHandler,
        UpdateUserFlagCommandHandler updateUserFlagHandler,
        GetUserByIdQueryHandler getUserByIdHandler
    ) {
        this.updatePreferencesHandler = updatePreferencesHandler;
        this.getPreferencesHandler = getPreferencesHandler;
        this.updateUserFlagHandler = updateUserFlagHandler;
        this.getUserByIdHandler = getUserByIdHandler;
    }

    /**
     * GET /api/user/preferences
     * Retrieves current user preferences.
     */
    @GetMapping("/preferences")
    public Mono<ResponseEntity<UserPreferencesDTO>> getPreferences(@AuthenticationPrincipal UserPrincipal currentUser) {
        GetUserPreferencesQuery query = new GetUserPreferencesQuery(currentUser.userId());

        return getPreferencesHandler.handle(query)
            .map(ResponseEntity::ok)
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity.notFound().build())
            );
    }

    /**
     * PUT /api/user/preferences
     * Updates user preferences.
     */
    @PutMapping("/preferences")
    public Mono<ResponseEntity<UserPreferencesDTO>> updatePreferences(
        @Valid @RequestBody UpdatePreferencesRequest request,
        @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        UpdateUserPreferencesCommand command = new UpdateUserPreferencesCommand(
            currentUser.userId(),
            request.animationsEnabled(),
            request.soundEnabled(),
            request.theme(),
            request.concurrentUploads(),
            request.uploadCompleteNotifications(),
            request.autoRetryFailed()
        );

        return updatePreferencesHandler.handle(command)
            .map(ResponseEntity::ok)
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity.badRequest().build())
            );
    }

    /**
     * GET /api/user/profile
     * Retrieves current user profile.
     */
    @GetMapping("/profile")
    public Mono<ResponseEntity<UserDTO>> getProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        GetUserByIdQuery query = new GetUserByIdQuery(currentUser.userId());

        return getUserByIdHandler.handle(query)
            .map(ResponseEntity::ok)
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity.notFound().build())
            );
    }

    /**
     * PATCH /api/user/onboarding
     * Marks onboarding tutorial as complete for current user.
     */
    @PatchMapping("/onboarding")
    public Mono<ResponseEntity<UserDTO>> markOnboardingComplete(@AuthenticationPrincipal UserPrincipal currentUser) {
        UpdateUserFlagCommand command = new UpdateUserFlagCommand(currentUser.userId());

        return updateUserFlagHandler.handle(command)
            .map(UserDTO::fromDomain)
            .map(ResponseEntity::ok)
            .onErrorResume(IllegalArgumentException.class, e ->
                Mono.just(ResponseEntity.notFound().build())
            );
    }

    /**
     * Request DTO for update preferences endpoint.
     */
    public record UpdatePreferencesRequest(
        boolean animationsEnabled,
        boolean soundEnabled,

        @NotBlank(message = "Theme is required")
        String theme,

        @Min(value = 1, message = "Concurrent uploads must be at least 1")
        @Max(value = 20, message = "Concurrent uploads must be at most 20")
        int concurrentUploads,

        boolean uploadCompleteNotifications,
        boolean autoRetryFailed
    ) {}
}
