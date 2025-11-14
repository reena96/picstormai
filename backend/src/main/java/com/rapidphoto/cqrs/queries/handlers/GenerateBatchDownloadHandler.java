package com.rapidphoto.cqrs.queries.handlers;

import com.rapidphoto.cqrs.queries.GenerateBatchDownloadQuery;
import com.rapidphoto.domain.photo.Photo;
import com.rapidphoto.domain.photo.PhotoRepository;
import com.rapidphoto.exception.BatchDownloadLimitExceededException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import org.springframework.beans.factory.annotation.Value;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Handler for batch photo download as ZIP.
 * Story 3.6: Batch Photo Download (ZIP)
 *
 * Streams ZIP file without loading entire content into memory.
 * Validates photo ownership and size limits before generating ZIP.
 */
@Component
public class GenerateBatchDownloadHandler {

    private static final long MAX_ZIP_SIZE_BYTES = 500L * 1024 * 1024; // 500MB
    private static final int BUFFER_SIZE = 8192;

    private final PhotoRepository photoRepository;
    private final S3Client s3Client;
    private final String bucketName;

    public GenerateBatchDownloadHandler(
        PhotoRepository photoRepository,
        S3Client s3Client,
        @Value("${aws.s3.bucket-name}") String bucketName
    ) {
        this.photoRepository = photoRepository;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /**
     * Handle batch download query - validates and generates ZIP stream.
     *
     * @param query Batch download query with photo IDs and user ID
     * @return Flux of DataBuffer containing ZIP file stream
     */
    public Flux<DataBuffer> handle(GenerateBatchDownloadQuery query) {
        return photoRepository.findAllById(query.photoIds())
            .collectList()
            .flatMapMany(photos -> {
                // Validate all photos exist
                if (photos.size() != query.photoIds().size()) {
                    return Flux.error(new PhotoNotFoundException("Some photos not found"));
                }

                // Validate all photos belong to user
                boolean allOwnedByUser = photos.stream()
                    .allMatch(photo -> photo.getUserId().equals(query.userId()));
                if (!allOwnedByUser) {
                    return Flux.error(new UnauthorizedException("Not authorized to download some photos"));
                }

                // Check total size limit
                long totalSize = photos.stream()
                    .mapToLong(Photo::getFileSize)
                    .sum();
                if (totalSize > MAX_ZIP_SIZE_BYTES) {
                    return Flux.error(new BatchDownloadLimitExceededException(
                        "Total size exceeds 500MB limit"));
                }

                // Generate ZIP stream
                return generateZipStream(photos);
            });
    }

    /**
     * Generate ZIP stream from list of photos.
     * Uses piped streams to avoid loading entire ZIP into memory.
     */
    private Flux<DataBuffer> generateZipStream(List<Photo> photos) {
        return Flux.create(sink -> {
            // Run ZIP generation on bounded elastic scheduler (blocking I/O)
            Schedulers.boundedElastic().schedule(() -> {
                try {
                    PipedOutputStream pipedOutputStream = new PipedOutputStream();
                    PipedInputStream pipedInputStream = new PipedInputStream(pipedOutputStream, BUFFER_SIZE * 4);

                    // Start ZIP generation in separate thread
                    Thread zipThread = new Thread(() -> {
                        try (ZipOutputStream zipOutputStream = new ZipOutputStream(pipedOutputStream)) {
                            Map<String, Integer> filenameCounts = new HashMap<>();

                            for (Photo photo : photos) {
                                // Get unique filename
                                String filename = getUniqueFilename(
                                    photo.getFilename(),
                                    filenameCounts
                                );

                                // Add ZIP entry
                                ZipEntry zipEntry = new ZipEntry(filename);
                                zipOutputStream.putNextEntry(zipEntry);

                                // Stream photo from S3
                                String s3Key = extractS3Key(photo);
                                try (InputStream photoStream = getS3ObjectStream(s3Key)) {
                                    // Copy to ZIP
                                    byte[] buffer = new byte[BUFFER_SIZE];
                                    int bytesRead;
                                    while ((bytesRead = photoStream.read(buffer)) != -1) {
                                        zipOutputStream.write(buffer, 0, bytesRead);
                                    }
                                }

                                zipOutputStream.closeEntry();
                            }
                        } catch (Exception e) {
                            try {
                                pipedOutputStream.close();
                            } catch (IOException ignored) {
                            }
                        }
                    });
                    zipThread.setName("zip-generator-" + UUID.randomUUID());
                    zipThread.start();

                    // Read from input stream and emit DataBuffers
                    DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int bytesRead;

                    try {
                        while ((bytesRead = pipedInputStream.read(buffer)) != -1) {
                            DataBuffer dataBuffer = bufferFactory.wrap(Arrays.copyOf(buffer, bytesRead));
                            sink.next(dataBuffer);
                        }
                        sink.complete();
                    } catch (IOException e) {
                        sink.error(e);
                    } finally {
                        try {
                            pipedInputStream.close();
                        } catch (IOException ignored) {
                        }
                        // Wait for ZIP thread to finish
                        try {
                            zipThread.join(30000); // 30 second timeout
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                        }
                    }
                } catch (Exception e) {
                    sink.error(e);
                }
            });
        });
    }

    /**
     * Get unique filename by appending suffix if duplicate exists.
     * Example: photo.jpg, photo-1.jpg, photo-2.jpg
     */
    private String getUniqueFilename(String originalFilename, Map<String, Integer> counts) {
        if (!counts.containsKey(originalFilename)) {
            counts.put(originalFilename, 1);
            return originalFilename;
        }

        // Extract base name and extension
        int dotIndex = originalFilename.lastIndexOf('.');
        String baseName;
        String extension;

        if (dotIndex > 0) {
            baseName = originalFilename.substring(0, dotIndex);
            extension = originalFilename.substring(dotIndex);
        } else {
            baseName = originalFilename;
            extension = "";
        }

        int count = counts.get(originalFilename);
        counts.put(originalFilename, count + 1);
        return baseName + "-" + count + extension;
    }

    /**
     * Extract S3 key from photo.
     * Handles both full S3 URLs and direct keys.
     */
    private String extractS3Key(Photo photo) {
        if (photo.getS3Location() != null) {
            return photo.getS3Location().getKey();
        }
        // Fallback: extract from storage URL if needed
        String storageUrl = photo.getFilename(); // This should be s3Key
        if (storageUrl != null && storageUrl.contains("/uploads/")) {
            return storageUrl.substring(storageUrl.indexOf("/uploads/") + 1);
        }
        // Last resort: construct from user ID and filename
        return String.format("uploads/%s/%s", photo.getUserId(), photo.getFilename());
    }

    /**
     * Get InputStream from S3 for a given key.
     */
    private InputStream getS3ObjectStream(String s3Key) throws IOException {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

            ResponseInputStream<GetObjectResponse> response = s3Client.getObject(getObjectRequest);
            return response;
        } catch (Exception e) {
            throw new IOException("Failed to fetch S3 object: " + s3Key, e);
        }
    }

    /**
     * Exception thrown when photo is not found.
     */
    public static class PhotoNotFoundException extends RuntimeException {
        public PhotoNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when user is not authorized to access photos.
     */
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
