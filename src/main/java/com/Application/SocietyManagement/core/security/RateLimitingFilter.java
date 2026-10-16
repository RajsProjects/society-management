package com.Application.SocietyManagement.core.security;

import org.springframework.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;

    public RateLimitingFilter(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @org.springframework.beans.factory.annotation.Value("${security.rate-limit.max-requests:120}")
    private int maxRequests = 120;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        boolean isProtected = path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/societies/register")
                || path.startsWith("/api/v1/societies/join/")
                || path.startsWith("/api/v1/users/change-password")
                || path.startsWith("/api/v1/communication/email/test")
                || path.startsWith("/api/v1/subscriptions/create-order");

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
            }

            if (count != null && count > maxRequests) {
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