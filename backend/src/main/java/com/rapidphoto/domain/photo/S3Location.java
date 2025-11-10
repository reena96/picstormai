package com.rapidphoto.domain.photo;

import java.util.Objects;

/**
 * S3Location value object - immutable representation of S3 object location.
 * Contains bucket, key, and optional version ID.
 */
public final class S3Location {

    private final String bucket;
    private final String key;
    private final String versionId;

    private S3Location(String bucket, String key, String versionId) {
        this.bucket = bucket;
        this.key = key;
        this.versionId = versionId;
    }

    /**
     * Create S3Location with bucket and key (no version).
     */
    public static S3Location of(String bucket, String key) {
        return of(bucket, key, null);
    }

    /**
     * Create S3Location with bucket, key, and version ID.
     */
    public static S3Location of(String bucket, String key, String versionId) {
        if (bucket == null || bucket.trim().isEmpty()) {
            throw new IllegalArgumentException("S3 bucket cannot be null or empty");
        }
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("S3 key cannot be null or empty");
        }

        return new S3Location(bucket.trim(), key.trim(), versionId);
    }

    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }

    public String getVersionId() {
        return versionId;
    }

    public boolean hasVersion() {
        return versionId != null && !versionId.isEmpty();
    }

    /**
     * Get full S3 URI (s3://bucket/key).
     */
    public String toUri() {
        return "s3://" + bucket + "/" + key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        S3Location that = (S3Location) o;
        return Objects.equals(bucket, that.bucket) &&
               Objects.equals(key, that.key) &&
               Objects.equals(versionId, that.versionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucket, key, versionId);
    }

    @Override
    public String toString() {
        return hasVersion()
            ? String.format("s3://%s/%s?versionId=%s", bucket, key, versionId)
            : String.format("s3://%s/%s", bucket, key);
    }
}
