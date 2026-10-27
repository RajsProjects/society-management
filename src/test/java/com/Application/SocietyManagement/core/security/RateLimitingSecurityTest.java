package com.Application.SocietyManagement.core.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security: Rate Limiting & Abuse Resistance Test Suite")
class RateLimitingSecurityTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        rateLimitingFilter = new RateLimitingFilter(redisTemplate);
        ReflectionTestUtils.setField(rateLimitingFilter, "maxRequests", 5);
    }

    @Test
    @DisplayName("Rate Limiter: Requests within threshold are permitted through filter chain")
    void requestsWithinLimit_arePermitted() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(3L);

        rateLimitingFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Rate Limiter: First request in window initializes Redis TTL expiration")
    void firstRequest_setsTtl() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        rateLimitingFilter.doFilter(request, response, filterChain);

        verify(redisTemplate).expire(eq("rate_limit:192.168.1.50:/api/v1/auth/login"), eq(Duration.ofMinutes(1)));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Rate Limiter: Requests exceeding maxRequests receive 429 Too Many Requests")
    void requestsExceedingLimit_throttledWith429() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(6L); // > 5

        rateLimitingFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("Too many requests. Please try again later.");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("IP Extraction Security: Untrusted client-supplied X-Forwarded-For header is not blindly trusted")
    void spoofedXForwardedForHeader_doesNotBypassRateLimiter() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.168.1.50");
        // Attacker attempts to spoof a rotating proxy IP in header
        request.addHeader("X-Forwarded-For", "203.0.113.199");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);

        rateLimitingFilter.doFilter(request, response, filterChain);

        // Asserts key was built using the remote address, preventing trivial header spoofing
        verify(valueOperations).increment("rate_limit:192.168.1.50:/api/v1/auth/login");
    }

    @Test
    @DisplayName("Resilience Fallback: Redis connection failure fails open without crashing the API")
    void redisFailure_failsOpenGracefully() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Redis connection timed out"));

        rateLimitingFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Path Scope: Non-sensitive read endpoints bypass rate limiting entirely")
    void unprotectedEndpoints_bypassRateLimiting() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(request, response, filterChain);

        verify(redisTemplate, never()).opsForValue();
        verify(filterChain).doFilter(request, response);
    }
}

