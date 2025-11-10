package com.rapidphoto.domain.photo;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class PhotoIdTest {

    @Test
    void shouldCreatePhotoIdFromUUID() {
        UUID uuid = UUID.randomUUID();
        PhotoId photoId = PhotoId.of(uuid);
        assertThat(photoId.getValue()).isEqualTo(uuid);
    }

    @Test
    void shouldGenerateNewPhotoId() {
        PhotoId photoId1 = PhotoId.generate();
        PhotoId photoId2 = PhotoId.generate();

        assertThat(photoId1).isNotNull();
        assertThat(photoId2).isNotNull();
        assertThat(photoId1).isNotEqualTo(photoId2);
    }

    @Test
    void shouldCreateFromValidString() {
        String uuidString = "123e4567-e89b-12d3-a456-426614174000";
        PhotoId photoId = PhotoId.fromString(uuidString);
        assertThat(photoId.getValue().toString()).isEqualTo(uuidString);
    }

    @Test
    void shouldRejectNullUUID() {
        assertThatThrownBy(() -> PhotoId.of(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cannot be null");
    }

    @Test
    void shouldRejectInvalidStringFormat() {
        assertThatThrownBy(() -> PhotoId.fromString("invalid-uuid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid PhotoId format");
    }

    @Test
    void shouldSupportEquality() {
        UUID uuid = UUID.randomUUID();
        PhotoId photoId1 = PhotoId.of(uuid);
        PhotoId photoId2 = PhotoId.of(uuid);
        PhotoId photoId3 = PhotoId.generate();

        assertThat(photoId1).isEqualTo(photoId2);
        assertThat(photoId1).isNotEqualTo(photoId3);
        assertThat(photoId1.hashCode()).isEqualTo(photoId2.hashCode());
    }

    @Test
    void shouldConvertToString() {
        UUID uuid = UUID.randomUUID();
        PhotoId photoId = PhotoId.of(uuid);
        assertThat(photoId.toString()).isEqualTo(uuid.toString());
    }
}
