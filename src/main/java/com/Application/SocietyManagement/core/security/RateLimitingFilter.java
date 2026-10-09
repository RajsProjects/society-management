package com.Application.SocietyManagement.core.security;

import org.springframework.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;

    private static final int MAX_REQUESTS = 10;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        boolean isProtected = path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/societies/register")
                || path.startsWith("/api/v1/societies/join/");

        if (!isProtected) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String ip = extractClientIp(request);
            String normalizedPath = path.startsWith("/api/v1/societies/join/")
                    ? "/api/v1/societies/join"
                    : path;
            String key = "rate_limit:" + ip + ":" + normalizedPath;

            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, WINDOW);
            } else if (count != null && count > 1L) {
                Long ttl = redisTemplate.getExpire(key);
                if (ttl == null || ttl < 0) {
                    redisTemplate.expire(key, WINDOW);
                }
            }

            if (count != null && count > MAX_REQUESTS) {
                log.warn("Rate limit exceeded for IP: {} on path: {}", ip, path);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\": \"Too many requests. Please try again later.\"}");
                return;
            }

        } catch (Exception e) {
            // Redis unavailable — allow request, log warning
            log.warn("Rate limiting unavailable: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        // Rely on Spring Boot's internal proxy handling (server.forward-headers-strategy: framework)
        // and avoid blindly trusting raw client-supplied X-Forwarded-For headers to prevent rate limiting bypass.
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isBlank()) ? remoteAddr : "unknown";
    }
}