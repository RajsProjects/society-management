package com.Application.SocietyManagement.core.idempotency;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Persisted state of an idempotent request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdempotencyRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    private String key;
    private String requestHash;
    private IdempotencyStatus status;
    private int statusCode;
    private String contentType;
    private byte[] responseBody;
    private long createdAt;
    private long expiresAt;
}

