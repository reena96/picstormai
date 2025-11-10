package com.rapidphoto.cqrs.commands.handlers;

import com.rapidphoto.cqrs.commands.UpdateUserPreferencesCommand;
import com.rapidphoto.cqrs.dtos.UserPreferencesDTO;
import com.rapidphoto.domain.user.UserPreferences;
import com.rapidphoto.domain.user.UserPreferencesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateUserPreferencesCommandHandlerTest {

    private UserPreferencesRepository preferencesRepository;
    private UpdateUserPreferencesCommandHandler handler;

    @BeforeEach
    void setUp() {
        preferencesRepository = mock(UserPreferencesRepository.class);
        handler = new UpdateUserPreferencesCommandHandler(preferencesRepository);
    }

    @Test
    @DisplayName("Should update existing preferences successfully")
    void shouldUpdateExistingPreferences() {
        // Given
        UUID userId = UUID.randomUUID();
        UserPreferences existingPreferences = UserPreferences.createDefault(userId);

        UpdateUserPreferencesCommand command = new UpdateUserPreferencesCommand(
            userId,
            false, // animationsEnabled
            false, // soundEnabled
            "DARK", // theme
            10, // concurrentUploads
            false, // uploadCompleteNotifications
            false  // autoRetryFailed
        );

        when(preferencesRepository.findByUserId(userId))
            .thenReturn(Mono.just(existingPreferences));
        when(preferencesRepository.save(any(UserPreferences.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When
        Mono<UserPreferencesDTO> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertEquals(userId, dto.userId());
                assertFalse(dto.animationsEnabled());
                assertFalse(dto.soundEnabled());
                assertEquals("DARK", dto.theme());
                assertEquals(10, dto.concurrentUploads());
                assertFalse(dto.uploadCompleteNotifications());
                assertFalse(dto.autoRetryFailed());
            })
            .verifyComplete();

        // Verify repository interactions
        verify(preferencesRepository).findByUserId(userId);
        verify(preferencesRepository).save(any(UserPreferences.class));

        // Verify the saved entity has correct values
        ArgumentCaptor<UserPreferences> captor = ArgumentCaptor.forClass(UserPreferences.class);
        verify(preferencesRepository).save(captor.capture());
        UserPreferences saved = captor.getValue();

        assertFalse(saved.isAnimationsEnabled());
        assertFalse(saved.isSoundEnabled());
        assertEquals(UserPreferences.Theme.DARK, saved.getTheme());
        assertEquals(10, saved.getConcurrentUploads());
        assertFalse(saved.isUploadCompleteNotifications());
        assertFalse(saved.isAutoRetryFailed());
    }

    @Test
    @DisplayName("Should create new preferences if not exists")
    void shouldCreateNewPreferencesIfNotExists() {
        // Given
        UUID userId = UUID.randomUUID();

        UpdateUserPreferencesCommand command = new UpdateUserPreferencesCommand(
            userId,
            true,
            true,
            "LIGHT",
            5,
            true,
            true
        );

        when(preferencesRepository.findByUserId(userId))
            .thenReturn(Mono.empty());
        when(preferencesRepository.save(any(UserPreferences.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // When
        Mono<UserPreferencesDTO> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertEquals(userId, dto.userId());
                assertTrue(dto.animationsEnabled());
                assertTrue(dto.soundEnabled());
                assertEquals("LIGHT", dto.theme());
                assertEquals(5, dto.concurrentUploads());
                assertTrue(dto.uploadCompleteNotifications());
                assertTrue(dto.autoRetryFailed());
            })
            .verifyComplete();

        verify(preferencesRepository).save(any(UserPreferences.class));
    }

    @Test
    @DisplayName("Should reject invalid theme")
    void shouldRejectInvalidTheme() {
        // Given
        UUID userId = UUID.randomUUID();

        UpdateUserPreferencesCommand command = new UpdateUserPreferencesCommand(
            userId,
            true,
            true,
            "INVALID_THEME",
            5,
            true,
            true
        );

        when(preferencesRepository.findByUserId(userId))
            .thenReturn(Mono.empty());

        // When
        Mono<UserPreferencesDTO> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Invalid theme")
            )
            .verify();

        verify(preferencesRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject concurrent uploads below minimum (0)")
    void shouldRejectConcurrentUploadsBelowMinimum() {
        // Given
        UUID userId = UUID.randomUUID();
        UserPreferences existingPreferences = UserPreferences.createDefault(userId);

        UpdateUserPreferencesCommand command = new UpdateUserPreferencesCommand(
            userId,
            true,
            true,
            "LIGHT",
            0, // Invalid: below minimum
            true,
            true
        );

        when(preferencesRepository.findByUserId(userId))
            .thenReturn(Mono.just(existingPreferences));

        // When
        Mono<UserPreferencesDTO> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Concurrent uploads must be between 1 and 20")
            )
            .verify();

        verify(preferencesRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject concurrent uploads above maximum (21)")
    void shouldRejectConcurrentUploadsAboveMaximum() {
        // Given
        UUID userId = UUID.randomUUID();
        UserPreferences existingPreferences = UserPreferences.createDefault(userId);

        UpdateUserPreferencesCommand command = new UpdateUserPreferencesCommand(
            userId,
            true,
            true,
            "LIGHT",
            21, // Invalid: above maximum
            true,
            true
        );

        when(preferencesRepository.findByUserId(userId))
            .thenReturn(Mono.just(existingPreferences));

        // When
        Mono<UserPreferencesDTO> result = handler.handle(command);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("Concurrent uploads must be between 1 and 20")
            )
            .verify();

        verify(preferencesRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should accept boundary values for concurrent uploads")
    void shouldAcceptBoundaryValuesForConcurrentUploads() {
        // Given
        UUID userId = UUID.randomUUID();
        UserPreferences existingPreferences = UserPreferences.createDefault(userId);

        when(preferencesRepository.findByUserId(userId))
            .thenReturn(Mono.just(existingPreferences));
        when(preferencesRepository.save(any(UserPreferences.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Test minimum (1)
        UpdateUserPreferencesCommand commandMin = new UpdateUserPreferencesCommand(
            userId, true, true, "LIGHT", 1, true, true
        );

        StepVerifier.create(handler.handle(commandMin))
            .assertNext(dto -> assertEquals(1, dto.concurrentUploads()))
            .verifyComplete();

        // Test maximum (20)
        UpdateUserPreferencesCommand commandMax = new UpdateUserPreferencesCommand(
            userId, true, true, "LIGHT", 20, true, true
        );

        StepVerifier.create(handler.handle(commandMax))
            .assertNext(dto -> assertEquals(20, dto.concurrentUploads()))
            .verifyComplete();
    }

    @Test
    @DisplayName("Should accept theme values in any case")
    void shouldAcceptThemeValuesInAnyCase() {
        // Given
        UUID userId = UUID.randomUUID();
        UserPreferences existingPreferences = UserPreferences.createDefault(userId);

        when(preferencesRepository.findByUserId(userId))
            .thenReturn(Mono.just(existingPreferences));
        when(preferencesRepository.save(any(UserPreferences.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        // Test lowercase
        UpdateUserPreferencesCommand commandLower = new UpdateUserPreferencesCommand(
            userId, true, true, "light", 5, true, true
        );

        StepVerifier.create(handler.handle(commandLower))
            .assertNext(dto -> assertEquals("LIGHT", dto.theme()))
            .verifyComplete();

        // Test mixed case
        UpdateUserPreferencesCommand commandMixed = new UpdateUserPreferencesCommand(
            userId, true, true, "Dark", 5, true, true
        );

        StepVerifier.create(handler.handle(commandMixed))
            .assertNext(dto -> assertEquals("DARK", dto.theme()))
            .verifyComplete();
    }
}
