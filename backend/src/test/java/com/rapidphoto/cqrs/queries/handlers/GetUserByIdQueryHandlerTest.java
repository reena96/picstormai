package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.UserDTO;
import com.rapidphoto.cqrs.queries.GetUserByIdQuery;
import com.rapidphoto.domain.shared.Email;
import com.rapidphoto.domain.user.User;
import com.rapidphoto.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetUserByIdQueryHandlerTest {

    @Mock
    private UserRepository userRepository;

    private GetUserByIdQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetUserByIdQueryHandler(userRepository);
    }

    @Test
    void shouldReturnUserDTO() {
        // Given
        User user = User.create(Email.of("test@example.com"), "password123", "Test User");
        GetUserByIdQuery query = new GetUserByIdQuery(user.getId());

        when(userRepository.findById(user.getId())).thenReturn(Mono.just(user));

        // When
        Mono<UserDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertThat(dto.id()).isEqualTo(user.getId());
                assertThat(dto.email()).isEqualTo("test@example.com");
                assertThat(dto.displayName()).isEqualTo("Test User");
                assertThat(dto.emailVerified()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    void shouldFailWhenUserNotFound() {
        // Given
        UUID userId = UUID.randomUUID();
        GetUserByIdQuery query = new GetUserByIdQuery(userId);

        when(userRepository.findById(userId)).thenReturn(Mono.empty());

        // When
        Mono<UserDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .expectErrorMatches(throwable ->
                throwable instanceof IllegalArgumentException &&
                throwable.getMessage().contains("User not found")
            )
            .verify();
    }
}
