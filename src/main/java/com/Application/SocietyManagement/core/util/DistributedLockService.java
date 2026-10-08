package com.Application.SocietyManagement.core.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Provides Redis-backed distributed locks for scheduled background tasks
 * to prevent duplicate execution when scaled to multiple replicas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockService {

    @Autowired(required = false)
    private final RedisTemplate<String, String> redisTemplate;

    public boolean tryLock(String lockName, Duration duration) {
        if (redisTemplate == null) {
            return true; // Single-instance or dev mode without Redis: grant lock
        }
        try {
            String key = "dist_lock:" + lockName;
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "locked", duration);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("DistributedLockService unable to reach Redis for lock {}: {}. Proceeding safely.", lockName, e.getMessage());
            return true;
        }
    }

    public void releaseLock(String lockName) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete("dist_lock:" + lockName);
        } catch (Exception e) {
            log.debug("Failed to release lock {}: {}", lockName, e.getMessage());
        }
    }
}
