package com.Application.SocietyManagement.core.idempotency;

import lombok.Getter;

/**
 * Result of claiming an idempotency key.
 */
@Getter
public class IdempotencyClaimResult {

    public enum Type {
        NEW_OPERATION,         // Key claimed; proceed to execute downstream
        COMPLETED_REPLAY,      // Found completed record; replay cached response
        IN_PROGRESS_CONFLICT,  // Key currently in progress by another thread/request (409)
        PAYLOAD_MISMATCH       // Key was previously used with a different request payload (422)
    }

    private final Type type;
    private final IdempotencyRecord record;

    public IdempotencyClaimResult(Type type, IdempotencyRecord record) {
        this.type = type;
        this.record = record;
    }

    public static IdempotencyClaimResult newOperation() {
        return new IdempotencyClaimResult(Type.NEW_OPERATION, null);
    }

    public static IdempotencyClaimResult completed(IdempotencyRecord record) {
        return new IdempotencyClaimResult(Type.COMPLETED_REPLAY, record);
    }

    public static IdempotencyClaimResult inProgress() {
        return new IdempotencyClaimResult(Type.IN_PROGRESS_CONFLICT, null);
    }

    public static IdempotencyClaimResult payloadMismatch() {
        return new IdempotencyClaimResult(Type.PAYLOAD_MISMATCH, null);
    }
}

