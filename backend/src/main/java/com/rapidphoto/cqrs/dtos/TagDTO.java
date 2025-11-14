package com.rapidphoto.cqrs.dtos;

import com.rapidphoto.domain.tag.Tag;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Tag data.
 * Never expose domain entities directly - always use DTOs.
 */
public record TagDTO(
    UUID id,
    String name,
    String color,
    Instant createdAt
) {
    /**
     * Create DTO from domain entity.
     */
    public static TagDTO fromDomain(Tag tag) {
        return new TagDTO(
            tag.getId(),
            tag.getName(),
            tag.getColor(),
            tag.getCreatedAt()
        );
    }

    /**
     * Create DTO from repository projection.
     */
    public static TagDTO from(UUID id, String name, String color, Instant createdAt) {
        return new TagDTO(id, name, color, createdAt);
    }
}
