package com.Application.SocietyManagement.core.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing idempotency records with primary Redis backing and
 * automatic thread-safe in-memory fallback when Redis is absent or unreachable.
 */
@Slf4j
@Service
public class IdempotencyService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, IdempotencyRecord> inMemoryStore = new ConcurrentHashMap<>();

    public IdempotencyService(
            @Autowired(required = false) RedisTemplate<String, String> redisTemplate,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Atomically claims an idempotency key or returns the status of an existing request.
     */
    public IdempotencyClaimResult claimKey(String scopedKey, String requestHash, long ttlSeconds) {
        long now = System.currentTimeMillis();
        long expiresAt = now + (ttlSeconds * 1000);

        if (redisTemplate != null) {
            try {
                IdempotencyRecord inProgressRecord = IdempotencyRecord.builder()
                        .key(scopedKey)
                        .requestHash(requestHash)
                        .status(IdempotencyStatus.IN_PROGRESS)
                        .createdAt(now)
                        .expiresAt(expiresAt)
                        .build();

                String inProgressJson = objectMapper.writeValueAsString(inProgressRecord);
                Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                        scopedKey,
                        inProgressJson,
                        Duration.ofSeconds(ttlSeconds)
                );

                if (Boolean.TRUE.equals(acquired)) {
                    return IdempotencyClaimResult.newOperation();
                }

                // Key already exists in Redis, inspect existing record
                String existingJson = redisTemplate.opsForValue().get(scopedKey);
                if (existingJson != null) {
                    IdempotencyRecord existing = objectMapper.readValue(existingJson, IdempotencyRecord.class);
                    if (!requestHash.equals(existing.getRequestHash())) {
                        return IdempotencyClaimResult.payloadMismatch();
                    }
                    if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
                        return IdempotencyClaimResult.inProgress();
                    }
                    return IdempotencyClaimResult.completed(existing);
                }
            } catch (Exception e) {
                log.warn("Redis error in claimKey for {}: {}. Falling back to in-memory store.", scopedKey, e.getMessage());
            }
        }

        // In-memory fallback
        evictExpiredInMemory(now);
        IdempotencyRecord inProgressRecord = IdempotencyRecord.builder()
                .key(scopedKey)
                .requestHash(requestHash)
                .status(IdempotencyStatus.IN_PROGRESS)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        IdempotencyRecord existing = inMemoryStore.putIfAbsent(scopedKey, inProgressRecord);
        if (existing == null) {
            return IdempotencyClaimResult.newOperation();
        }

        // If existing record was expired, overwrite it
        if (existing.getExpiresAt() <= now) {
            inMemoryStore.put(scopedKey, inProgressRecord);
            return IdempotencyClaimResult.newOperation();
        }

        if (!requestHash.equals(existing.getRequestHash())) {
            return IdempotencyClaimResult.payloadMismatch();
        }

        if (existing.getStatus() == IdempotencyStatus.IN_PROGRESS) {
            return IdempotencyClaimResult.inProgress();
        }

        return IdempotencyClaimResult.completed(existing);
    }

    /**
     * Marks the idempotent operation as completed, caching the final response.
     */
    public void complete(String scopedKey, String requestHash, int statusCode, String contentType, byte[] responseBody, long ttlSeconds) {
        long now = System.currentTimeMillis();
        long expiresAt = now + (ttlSeconds * 1000);

        IdempotencyRecord record = IdempotencyRecord.builder()
                .key(scopedKey)
                .requestHash(requestHash)
                .status(IdempotencyStatus.COMPLETED)
                .statusCode(statusCode)
                .contentType(contentType)
                .responseBody(responseBody)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();

        if (redisTemplate != null) {
            try {
                String json = objectMapper.writeValueAsString(record);
                redisTemplate.opsForValue().set(scopedKey, json, Duration.ofSeconds(ttlSeconds));
                return;
            } catch (Exception e) {
                log.warn("Redis error in complete for {}: {}. Storing in in-memory store.", scopedKey, e.getMessage());
            }
        }

        inMemoryStore.put(scopedKey, record);
    }

    /**
     * Releases an in-progress or failed idempotency key so future requests can retry.
     */
    public void releaseKey(String scopedKey) {
        if (redisTemplate != null) {
            try {
                redisTemplate.delete(scopedKey);
            } catch (Exception e) {
                log.warn("Redis error in releaseKey for {}: {}", scopedKey, e.getMessage());
            }
        }
        inMemoryStore.remove(scopedKey);
    }

    /**
     * Clear all in-memory entries (useful for test isolation).
     */
    public void clearInMemoryStore() {
        inMemoryStore.clear();
    }

    private void evictExpiredInMemory(long now) {
        inMemoryStore.entrySet().removeIf(entry -> entry.getValue().getExpiresAt() <= now);
    }
}

