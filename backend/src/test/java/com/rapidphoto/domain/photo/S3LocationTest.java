package com.rapidphoto.domain.photo;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class S3LocationTest {

    @Test
    void shouldCreateS3LocationWithBucketAndKey() {
        S3Location location = S3Location.of("my-bucket", "photos/photo1.jpg");
        assertThat(location.getBucket()).isEqualTo("my-bucket");
        assertThat(location.getKey()).isEqualTo("photos/photo1.jpg");
        assertThat(location.getVersionId()).isNull();
        assertThat(location.hasVersion()).isFalse();
    }

    @Test
    void shouldCreateS3LocationWithVersionId() {
        S3Location location = S3Location.of("my-bucket", "photos/photo1.jpg", "v123");
        assertThat(location.getBucket()).isEqualTo("my-bucket");
        assertThat(location.getKey()).isEqualTo("photos/photo1.jpg");
        assertThat(location.getVersionId()).isEqualTo("v123");
        assertThat(location.hasVersion()).isTrue();
    }

    @Test
    void shouldTrimBucketAndKey() {
        S3Location location = S3Location.of("  my-bucket  ", "  photos/photo1.jpg  ");
        assertThat(location.getBucket()).isEqualTo("my-bucket");
        assertThat(location.getKey()).isEqualTo("photos/photo1.jpg");
    }

    @Test
    void shouldRejectNullBucket() {
        assertThatThrownBy(() -> S3Location.of(null, "key"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bucket cannot be null");
    }

    @Test
    void shouldRejectEmptyBucket() {
        assertThatThrownBy(() -> S3Location.of("", "key"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("bucket cannot be null or empty");
    }

    @Test
    void shouldRejectNullKey() {
        assertThatThrownBy(() -> S3Location.of("bucket", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("key cannot be null");
    }

    @Test
    void shouldRejectEmptyKey() {
        assertThatThrownBy(() -> S3Location.of("bucket", ""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("key cannot be null or empty");
    }

    @Test
    void shouldGenerateUriWithoutVersion() {
        S3Location location = S3Location.of("my-bucket", "photos/photo1.jpg");
        assertThat(location.toUri()).isEqualTo("s3://my-bucket/photos/photo1.jpg");
    }

    @Test
    void shouldSupportEquality() {
        S3Location loc1 = S3Location.of("bucket", "key", "v1");
        S3Location loc2 = S3Location.of("bucket", "key", "v1");
        S3Location loc3 = S3Location.of("bucket", "key", "v2");
        S3Location loc4 = S3Location.of("bucket", "key");

        assertThat(loc1).isEqualTo(loc2);
        assertThat(loc1).isNotEqualTo(loc3);
        assertThat(loc1).isNotEqualTo(loc4);
        assertThat(loc1.hashCode()).isEqualTo(loc2.hashCode());
    }

    @Test
    void shouldConvertToStringWithVersion() {
        S3Location location = S3Location.of("bucket", "key", "v123");
        assertThat(location.toString()).isEqualTo("s3://bucket/key?versionId=v123");
    }

    @Test
    void shouldConvertToStringWithoutVersion() {
        S3Location location = S3Location.of("bucket", "key");
        assertThat(location.toString()).isEqualTo("s3://bucket/key");
    }
}
