package com.rapidphoto.exception;

/**
 * Exception thrown when batch download exceeds size or count limits.
 * Story 3.6: Batch Photo Download (ZIP)
 */
public class BatchDownloadLimitExceededException extends RuntimeException {
    public BatchDownloadLimitExceededException(String message) {
        super(message);
    }
}
