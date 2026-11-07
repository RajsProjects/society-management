package com.Application.SocietyManagement.core.idempotency;

import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security & Reliability: Idempotency Key Framework Audit Test Suite")
class IdempotencySecurityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private IdempotencyService idempotencyService;
    private IdempotencyFilter idempotencyFilter;

    @Mock
    private RequestMappingHandlerMapping handlerMapping;

    @BeforeEach
    void setUp() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        // Standalone service with in-memory fallback enabled
        idempotencyService = new IdempotencyService(null, objectMapper);
        idempotencyFilter = new IdempotencyFilter(idempotencyService, handlerMapping, objectMapper);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
        idempotencyService.clearInMemoryStore();
    }

    private void authenticateUser(String userId, String societyId) {
        TenantContext.setSocietyId(societyId);
        User mockUser = new User();
        mockUser.setId(userId);
        mockUser.setEmail("resident@civiclink.in");
        mockUser.setRole(Roles.RESIDENT);

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(mockUser, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Idempotency Replay: Duplicate POST request returns cached response without duplicate execution")
    void duplicateRequest_replaysCachedResponseWithoutExecutingDownstream() throws Exception {
        authenticateUser("user-101", "society-Alpha");

        String idempotencyKey = "key-complaint-001";
        String requestBody = "{\"title\":\"Water Leak\",\"description\":\"Pipe broken in flat 302\"}";
        String responseBody = "{\"id\":\"comp-999\",\"status\":\"OPEN\"}";

        // First Request
        MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/v1/complaints");
        request1.addHeader("Idempotency-Key", idempotencyKey);
        request1.setContentType("application/json");
        request1.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response1 = new MockHttpServletResponse();

        FilterChain filterChain1 = (req, res) -> {
            HttpServletResponse httpRes = (HttpServletResponse) res;
            httpRes.setStatus(201);
            httpRes.setContentType("application/json");
            httpRes.getOutputStream().write(responseBody.getBytes(StandardCharsets.UTF_8));
        };

        idempotencyFilter.doFilter(request1, response1, filterChain1);

        assertThat(response1.getStatus()).isEqualTo(201);
        assertThat(response1.getHeader("Idempotent-Replayed")).isEqualTo("false");
        assertThat(response1.getHeader("Idempotency-Key")).isEqualTo(idempotencyKey);
        assertThat(response1.getContentAsString()).isEqualTo(responseBody);

        // Second Request: Exactly identical key, path, and payload
        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/v1/complaints");
        request2.addHeader("Idempotency-Key", idempotencyKey);
        request2.setContentType("application/json");
        request2.setContent(requestBody.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        FilterChain filterChain2 = mock(FilterChain.class);

        idempotencyFilter.doFilter(request2, response2, filterChain2);

        // Downstream chain MUST NOT have been invoked on replay!
        verify(filterChain2, never()).doFilter(any(), any());

        // Replayed response matches the original
        assertThat(response2.getStatus()).isEqualTo(201);
        assertThat(response2.getHeader("Idempotent-Replayed")).isEqualTo("true");
        assertThat(response2.getHeader("Idempotency-Key")).isEqualTo(idempotencyKey);
        assertThat(response2.getContentAsString()).isEqualTo(responseBody);
    }

    @Test
    @DisplayName("Anti-Tampering (RFC 9457 / Stripe standard): Reusing same key with different body returns 422 Unprocessable Entity")
    void payloadMismatch_returns422UnprocessableEntity() throws Exception {
        authenticateUser("user-101", "society-Alpha");

        String idempotencyKey = "key-tamper-test";
        String originalBody = "{\"amount\":1000,\"reference\":\"UPI-TXN-111\"}";
        String tamperedBody = "{\"amount\":50000,\"reference\":\"UPI-TXN-999\"}";

        // Original request
        MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/v1/finance/bills/bill-1/pay");
        request1.addHeader("Idempotency-Key", idempotencyKey);
        request1.setContentType("application/json");
        request1.setContent(originalBody.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response1 = new MockHttpServletResponse();

        FilterChain filterChain1 = (req, res) -> {
            HttpServletResponse httpRes = (HttpServletResponse) res;
            httpRes.setStatus(200);
            httpRes.setContentType("application/json");
            httpRes.getOutputStream().write("{\"status\":\"PAID\"}".getBytes(StandardCharsets.UTF_8));
        };
        idempotencyFilter.doFilter(request1, response1, filterChain1);
        assertThat(response1.getStatus()).isEqualTo(200);

        // Tampered request with altered payload
        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/v1/finance/bills/bill-1/pay");
        request2.addHeader("Idempotency-Key", idempotencyKey);
        request2.setContentType("application/json");
        request2.setContent(tamperedBody.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        FilterChain filterChain2 = mock(FilterChain.class);
        idempotencyFilter.doFilter(request2, response2, filterChain2);

        verify(filterChain2, never()).doFilter(any(), any());
        assertThat(response2.getStatus()).isEqualTo(422);
        assertThat(response2.getContentAsString()).contains("Unprocessable Entity");
        assertThat(response2.getContentAsString()).contains("different request payload");
    }

    @Test
    @DisplayName("Concurrency Control: Concurrent in-flight request with same key returns 409 Conflict")
    void concurrentInFlightRequest_returns409Conflict() throws Exception {
        authenticateUser("user-200", "society-Beta");

        String idempotencyKey = "key-concurrent-lock";
        String body = "{\"plan\":\"ENTERPRISE\"}";

        // Manually claim the key in IN_PROGRESS state to simulate parallel executing thread
        String scopedKey = "idempotency:society-Beta:user-200:POST:/api/v1/subscriptions/create-order:" + idempotencyKey;
        String requestHash = "some-hash";
        idempotencyService.claimKey(scopedKey, requestHash, 300);

        // Second incoming request while first is still executing
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/subscriptions/create-order");
        request.addHeader("Idempotency-Key", idempotencyKey);
        request.setContentType("application/json");
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Ensure hash matches so it tests status check
        String computedHash = computeFilterHash(request, body.getBytes(StandardCharsets.UTF_8));
        idempotencyService.releaseKey(scopedKey);
        idempotencyService.claimKey(scopedKey, computedHash, 300);

        FilterChain filterChain = mock(FilterChain.class);
        idempotencyFilter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("Conflict");
        assertThat(response.getContentAsString()).contains("currently in progress");
    }

    @Test
    @DisplayName("Header Format: Accepts both Idempotency-Key and X-Idempotency-Key headers")
    void altHeader_xIdempotencyKey_isAcceptedAndCached() throws Exception {
        authenticateUser("user-300", "society-Gamma");

        String idempotencyKey = "alt-key-xyz-777";
        String body = "{\"name\":\"Power Outage Alert\"}";

        MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/v1/announcements");
        request1.addHeader("X-Idempotency-Key", idempotencyKey);
        request1.setContentType("application/json");
        request1.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response1 = new MockHttpServletResponse();

        FilterChain chain1 = (req, res) -> {
            HttpServletResponse httpRes = (HttpServletResponse) res;
            httpRes.setStatus(201);
            httpRes.setContentType("application/json");
            httpRes.getOutputStream().write("{\"id\":\"ann-1\"}".getBytes(StandardCharsets.UTF_8));
        };
        idempotencyFilter.doFilter(request1, response1, chain1);
        assertThat(response1.getStatus()).isEqualTo(201);

        // Replay with X-Idempotency-Key
        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/v1/announcements");
        request2.addHeader("X-Idempotency-Key", idempotencyKey);
        request2.setContentType("application/json");
        request2.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        FilterChain chain2 = mock(FilterChain.class);
        idempotencyFilter.doFilter(request2, response2, chain2);

        verify(chain2, never()).doFilter(any(), any());
        assertThat(response2.getStatus()).isEqualTo(201);
        assertThat(response2.getHeader("Idempotent-Replayed")).isEqualTo("true");
    }

    @Test
    @DisplayName("Validation: Idempotency-Key exceeding 256 characters returns 400 Bad Request")
    void oversizedKey_returns400BadRequest() throws Exception {
        String oversizedKey = "k".repeat(257);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/complaints");
        request.addHeader("Idempotency-Key", oversizedKey);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        idempotencyFilter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("exceeds maximum length");
    }

    @Test
    @DisplayName("Required Key: Endpoint with @Idempotent(required = true) rejects request missing key")
    void requiredKeyMissing_returns400BadRequest() throws Exception {
        // Mock handler with @Idempotent(required = true)
        Idempotent mockAnnotation = mock(Idempotent.class);
        when(mockAnnotation.required()).thenReturn(true);

        HandlerMethod mockHandlerMethod = mock(HandlerMethod.class);
        when(mockHandlerMethod.getMethodAnnotation(Idempotent.class)).thenReturn(mockAnnotation);

        HandlerExecutionChain chain = new HandlerExecutionChain(mockHandlerMethod);
        when(handlerMapping.getHandler(any())).thenReturn(chain);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/finance/critical-operation");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        idempotencyFilter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("Idempotency-Key header is required");
    }

    @Test
    @DisplayName("Safe HTTP Methods: GET requests with Idempotency-Key pass through unintercepted")
    void getRequests_passThroughUnintercepted() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/complaints");
        request.addHeader("Idempotency-Key", "any-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain filterChain = mock(FilterChain.class);
        idempotencyFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getHeader("Idempotent-Replayed")).isNull();
    }

    @Test
    @DisplayName("Failure Resilience: 5xx server errors do NOT cache failed state, allowing client retry")
    void serverError5xx_doesNotCacheFailure() throws Exception {
        authenticateUser("user-500", "society-Omega");
        String key = "key-server-error";
        String body = "{\"name\":\"test\"}";

        MockHttpServletRequest request1 = new MockHttpServletRequest("POST", "/api/v1/complaints");
        request1.addHeader("Idempotency-Key", key);
        request1.setContentType("application/json");
        request1.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response1 = new MockHttpServletResponse();

        FilterChain failingChain = (req, res) -> {
            HttpServletResponse httpRes = (HttpServletResponse) res;
            httpRes.setStatus(500);
            httpRes.getOutputStream().write("Internal Server Error".getBytes(StandardCharsets.UTF_8));
        };
        idempotencyFilter.doFilter(request1, response1, failingChain);
        assertThat(response1.getStatus()).isEqualTo(500);

        // Second retry request: server has recovered and returns 201
        MockHttpServletRequest request2 = new MockHttpServletRequest("POST", "/api/v1/complaints");
        request2.addHeader("Idempotency-Key", key);
        request2.setContentType("application/json");
        request2.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response2 = new MockHttpServletResponse();

        FilterChain recoveredChain = (req, res) -> {
            HttpServletResponse httpRes = (HttpServletResponse) res;
            httpRes.setStatus(201);
            httpRes.setContentType("application/json");
            httpRes.getOutputStream().write("{\"status\":\"CREATED\"}".getBytes(StandardCharsets.UTF_8));
        };
        idempotencyFilter.doFilter(request2, response2, recoveredChain);

        assertThat(response2.getStatus()).isEqualTo(201);
        assertThat(response2.getContentAsString()).contains("CREATED");
    }

    @Test
    @DisplayName("Tenant & User Isolation: Same idempotency key used by different tenants does not collide")
    void tenantIsolation_sameKeyDifferentTenants_doesNotCollide() throws Exception {
        String commonKey = "duplicate-key-string";
        String body = "{\"title\":\"Common Issue\"}";

        // Tenant 1 in Society A
        authenticateUser("user-tenant-1", "society-A");
        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/api/v1/issues");
        req1.addHeader("Idempotency-Key", commonKey);
        req1.setContentType("application/json");
        req1.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        idempotencyFilter.doFilter(req1, res1, (r, s) -> {
            ((HttpServletResponse) s).setStatus(201);
            s.getWriter().write("{\"id\":\"issue-society-A\"}");
        });
        assertThat(res1.getStatus()).isEqualTo(201);
        assertThat(res1.getContentAsString()).contains("issue-society-A");

        // Tenant 2 in Society B using SAME key string
        authenticateUser("user-tenant-2", "society-B");
        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/api/v1/issues");
        req2.addHeader("Idempotency-Key", commonKey);
        req2.setContentType("application/json");
        req2.setContent(body.getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        idempotencyFilter.doFilter(req2, res2, (r, s) -> {
            ((HttpServletResponse) s).setStatus(201);
            s.getWriter().write("{\"id\":\"issue-society-B\"}");
        });

        // Must succeed as independent new operation, NOT a replay of Tenant 1's record
        assertThat(res2.getStatus()).isEqualTo(201);
        assertThat(res2.getHeader("Idempotent-Replayed")).isEqualTo("false");
        assertThat(res2.getContentAsString()).contains("issue-society-B");
    }

    @Test
    @DisplayName("Redis Integration: IdempotencyService uses Redis operations when RedisTemplate is present")
    void redisIntegration_usesRedisOperations() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, String> mockRedis = mock(RedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> mockOps = mock(ValueOperations.class);
        when(mockRedis.opsForValue()).thenReturn(mockOps);

        IdempotencyService redisService = new IdempotencyService(mockRedis, objectMapper);

        // When setIfAbsent returns true -> new operation
        when(mockOps.setIfAbsent(eq("test-key"), anyString(), any(Duration.class))).thenReturn(true);
        IdempotencyClaimResult result = redisService.claimKey("test-key", "hash123", 60);
        assertThat(result.getType()).isEqualTo(IdempotencyClaimResult.Type.NEW_OPERATION);
        verify(mockOps).setIfAbsent(eq("test-key"), anyString(), eq(Duration.ofSeconds(60)));

        // Complete operation sets the value in Redis
        redisService.complete("test-key", "hash123", 200, "application/json", "{}".getBytes(StandardCharsets.UTF_8), 60);
        verify(mockOps).set(eq("test-key"), anyString(), eq(Duration.ofSeconds(60)));

        // Release key deletes the Redis key
        redisService.releaseKey("test-key");
        verify(mockRedis).delete("test-key");
    }

    private String computeFilterHash(HttpServletRequest request, byte[] body) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            digest.update(request.getMethod().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            digest.update(request.getRequestURI().getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            if (request.getQueryString() != null) {
                digest.update(request.getQueryString().getBytes(StandardCharsets.UTF_8));
            }
            digest.update((byte) ':');
            if (body != null) {
                digest.update(body);
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
