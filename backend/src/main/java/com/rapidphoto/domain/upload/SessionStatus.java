package com.rapidphoto.domain.upload;

/**
 * SessionStatus enum - represents upload session state.
 * Finite state machine: IN_PROGRESS -> COMPLETED/FAILED/CANCELLED
 */
public enum SessionStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}
