package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.dtos.TagDTO;
import com.rapidphoto.cqrs.queries.GetTagsForUserQuery;
import com.rapidphoto.domain.tag.Tag;
import com.rapidphoto.domain.tag.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for GetTagsForUserQueryHandler.
 * Story 3.3: Photo Tagging UI
 */
@ExtendWith(MockitoExtension.class)
class GetTagsForUserQueryHandlerTest {

    @Mock
    private TagRepository tagRepository;

    private GetTagsForUserQueryHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GetTagsForUserQueryHandler(tagRepository);
    }

    @Test
    void testReturnsAllTags_ForUser() {
        // Given
        UUID userId = UUID.randomUUID();

        Tag tag1 = Tag.create(userId, "vacation", "#3B82F6");
        Tag tag2 = Tag.create(userId, "family", "#EF4444");
        Tag tag3 = Tag.create(userId, "work", "#10B981");

        GetTagsForUserQuery query = new GetTagsForUserQuery(userId);

        when(tagRepository.findByUserId(userId)).thenReturn(Flux.just(tag1, tag2, tag3));

        // When
        Flux<TagDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertThat(dto.name()).isEqualTo("vacation");
                assertThat(dto.color()).isEqualTo("#3B82F6");
            })
            .assertNext(dto -> {
                assertThat(dto.name()).isEqualTo("family");
                assertThat(dto.color()).isEqualTo("#EF4444");
            })
            .assertNext(dto -> {
                assertThat(dto.name()).isEqualTo("work");
                assertThat(dto.color()).isEqualTo("#10B981");
            })
            .verifyComplete();
    }

    @Test
    void testReturnsEmpty_WhenNoTags() {
        // Given
        UUID userId = UUID.randomUUID();
        GetTagsForUserQuery query = new GetTagsForUserQuery(userId);

        when(tagRepository.findByUserId(userId)).thenReturn(Flux.empty());

        // When
        Flux<TagDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .verifyComplete();
    }

    @Test
    void testFiltersTagsByUser() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();

        Tag userTag = Tag.create(userId, "my-tag", "#3B82F6");

        GetTagsForUserQuery query = new GetTagsForUserQuery(userId);

        // Repository should only return tags for the specific user
        when(tagRepository.findByUserId(userId)).thenReturn(Flux.just(userTag));

        // When
        Flux<TagDTO> result = handler.handle(query);

        // Then
        StepVerifier.create(result)
            .assertNext(dto -> {
                assertThat(dto.name()).isEqualTo("my-tag");
                assertThat(dto.color()).isEqualTo("#3B82F6");
            })
            .verifyComplete();
    }
}
