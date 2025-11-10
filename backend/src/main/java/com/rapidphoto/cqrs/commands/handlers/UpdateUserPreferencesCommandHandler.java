package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.UpdateUserPreferencesCommand;
import com.rapidphoto.cqrs.dtos.UserPreferencesDTO;
import com.rapidphoto.domain.user.UserPreferences;
import com.rapidphoto.domain.user.UserPreferencesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Handler for UpdateUserPreferencesCommand.
 * Updates user preferences and persists to database.
 */
@Service
public class UpdateUserPreferencesCommandHandler {

    private final UserPreferencesRepository preferencesRepository;

    public UpdateUserPreferencesCommandHandler(UserPreferencesRepository preferencesRepository) {
        this.preferencesRepository = preferencesRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Mono<UserPreferencesDTO> handle(UpdateUserPreferencesCommand command) {
        // Validate theme enum
        UserPreferences.Theme theme;
        try {
            theme = UserPreferences.Theme.valueOf(command.theme().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Mono.error(new IllegalArgumentException("Invalid theme: " + command.theme()));
        }

        // Find or create preferences
        return preferencesRepository.findByUserId(command.userId())
            .switchIfEmpty(Mono.just(UserPreferences.createDefault(command.userId())))
            .flatMap(preferences -> {
                // Update all fields
                preferences.setAnimationsEnabled(command.animationsEnabled());
                preferences.setSoundEnabled(command.soundEnabled());
                preferences.setTheme(theme);

                // Validate and set concurrent uploads (throws if invalid)
                try {
                    preferences.setConcurrentUploads(command.concurrentUploads());
                } catch (IllegalArgumentException e) {
                    return Mono.error(e);
                }

                preferences.setUploadCompleteNotifications(command.uploadCompleteNotifications());
                preferences.setAutoRetryFailed(command.autoRetryFailed());

                // Save and return DTO
                return preferencesRepository.save(preferences)
                    .map(UserPreferencesDTO::fromDomain);
            });
    }
}
