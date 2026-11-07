package com.Application.SocietyManagement.core.idempotency;

import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.users.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Filter that intercepts mutating HTTP requests bearing an Idempotency-Key header.
 * Prevents duplicate processing, detects payload tampering (HTTP 422),
 * blocks concurrent duplicate execution (HTTP 409), and replays cached responses.
 */
@Slf4j
@Component
public class IdempotencyFilter extends OncePerRequestFilter {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String ALT_IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotent-Replayed";

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final long DEFAULT_TTL_SECONDS = 86400L; // 24 hours
    private static final int MAX_KEY_LENGTH = 256;

    private final IdempotencyService idempotencyService;
    private final RequestMappingHandlerMapping handlerMapping;
    private final ObjectMapper objectMapper;

    @Autowired
    public IdempotencyFilter(
            IdempotencyService idempotencyService,
            @Autowired(required = false) RequestMappingHandlerMapping handlerMapping,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.idempotencyService = idempotencyService;
        this.handlerMapping = handlerMapping;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // Safe/read-only HTTP methods (GET, HEAD, OPTIONS) are naturally idempotent
        if (!MUTATING_METHODS.contains(request.getMethod().toUpperCase())) {
            filterChain.doFilter(request, response);
            return;
        }

        Idempotent idempotentAnnotation = resolveIdempotentAnnotation(request);
        String idempotencyKey = extractKey(request);

        // If no idempotency key is supplied
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            if (idempotentAnnotation != null && idempotentAnnotation.required()) {
                writeErrorResponse(response, HttpStatus.BAD_REQUEST.value(), "Bad Request",
                        "Idempotency-Key header is required for this operation.", request.getRequestURI());
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        idempotencyKey = idempotencyKey.trim();
        if (idempotencyKey.length() > MAX_KEY_LENGTH) {
            writeErrorResponse(response, HttpStatus.BAD_REQUEST.value(), "Bad Request",
                    "Idempotency-Key exceeds maximum length of " + MAX_KEY_LENGTH + " characters.", request.getRequestURI());
            return;
        }

        // Avoid buffering full payloads for multipart file uploads
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE.toLowerCase())) {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        byte[] requestBody = wrappedRequest.getCachedBody();
        String requestHash = computeRequestHash(
                wrappedRequest.getMethod(),
                wrappedRequest.getRequestURI(),
                wrappedRequest.getQueryString(),
                requestBody
        );

        long ttlSeconds = (idempotentAnnotation != null && idempotentAnnotation.ttlSeconds() > 0)
                ? idempotentAnnotation.ttlSeconds()
                : DEFAULT_TTL_SECONDS;

        String scopedKey = buildScopedKey(wrappedRequest, idempotencyKey);
        IdempotencyClaimResult claim = idempotencyService.claimKey(scopedKey, requestHash, ttlSeconds);

        if (claim.getType() == IdempotencyClaimResult.Type.PAYLOAD_MISMATCH) {
            writeErrorResponse(response, 422, "Unprocessable Entity",
                    "Idempotency-Key was previously used with a different request payload or target.", wrappedRequest.getRequestURI());
            return;
        }

        if (claim.getType() == IdempotencyClaimResult.Type.IN_PROGRESS_CONFLICT) {
            writeErrorResponse(response, HttpStatus.CONFLICT.value(), "Conflict",
                    "A request with this Idempotency-Key is currently in progress. Please retry after the original request completes.", wrappedRequest.getRequestURI());
            return;
        }

        if (claim.getType() == IdempotencyClaimResult.Type.COMPLETED_REPLAY) {
            IdempotencyRecord record = claim.getRecord();
            replayCachedResponse(response, record, idempotencyKey);
            return;
        }

        // Fresh request: execute downstream and cache the response
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        boolean executionSuccessful = false;
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
            executionSuccessful = true;
        } finally {
            if (!executionSuccessful) {
                // If an unexpected error or runtime exception occurs, release the key so retry is possible
                idempotencyService.releaseKey(scopedKey);
            }
        }

        int statusCode = wrappedResponse.getStatus();
        if (statusCode >= 500) {
            // Transient 5xx server errors must not permanently cache failures
            idempotencyService.releaseKey(scopedKey);
        } else {
            // Deterministic outcomes (2xx, 3xx, 4xx) are saved as completed
            idempotencyService.complete(
                    scopedKey,
                    requestHash,
                    statusCode,
                    wrappedResponse.getContentType(),
                    wrappedResponse.getContentAsByteArray(),
                    ttlSeconds
            );
        }

        wrappedResponse.setHeader(REPLAYED_HEADER, "false");
        wrappedResponse.setHeader(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        wrappedResponse.copyBodyToResponse();
    }

    private void replayCachedResponse(HttpServletResponse response, IdempotencyRecord record, String idempotencyKey) throws IOException {
        response.setStatus(record.getStatusCode());
        if (record.getContentType() != null && !record.getContentType().isBlank()) {
            response.setContentType(record.getContentType());
        }
        response.setHeader(REPLAYED_HEADER, "true");
        response.setHeader(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
        if (record.getResponseBody() != null && record.getResponseBody().length > 0) {
            response.getOutputStream().write(record.getResponseBody());
        }
        response.flushBuffer();
    }

    private String extractKey(HttpServletRequest request) {
        String key = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (key == null || key.isBlank()) {
            key = request.getHeader(ALT_IDEMPOTENCY_KEY_HEADER);
        }
        return key;
    }

    private String buildScopedKey(HttpServletRequest request, String key) {
        String societyId = TenantContext.getSocietyId();
        if (societyId == null || societyId.isBlank()) {
            societyId = "global";
        }

        String principalId = "anonymous";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            Object principal = auth.getPrincipal();
            if (principal instanceof User user && user.getId() != null) {
                principalId = user.getId();
            } else {
                principalId = auth.getName();
            }
        }

        return "idempotency:" + societyId + ":" + principalId + ":" + request.getMethod().toUpperCase() + ":" + request.getRequestURI() + ":" + key;
    }

    private String computeRequestHash(String method, String uri, String queryString, byte[] body) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(method.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            digest.update(uri.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) ':');
            if (queryString != null && !queryString.isBlank()) {
                digest.update(queryString.getBytes(StandardCharsets.UTF_8));
            }
            digest.update((byte) ':');
            if (body != null && body.length > 0) {
                digest.update(body);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private Idempotent resolveIdempotentAnnotation(HttpServletRequest request) {
        if (handlerMapping == null) {
            return null;
        }
        try {
            HandlerExecutionChain chain = handlerMapping.getHandler(request);
            if (chain != null && chain.getHandler() instanceof HandlerMethod handlerMethod) {
                Idempotent annotation = handlerMethod.getMethodAnnotation(Idempotent.class);
                if (annotation == null) {
                    annotation = handlerMethod.getBeanType().getAnnotation(Idempotent.class);
                }
                return annotation;
            }
        } catch (Exception e) {
            log.trace("Unable to inspect handler method for idempotency annotation: {}", e.getMessage());
        }
        return null;
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String error, String message, String path) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = Map.of(
                "timestamp", Instant.now().toString(),
                "status", status,
                "error", error,
                "message", message,
                "path", path
        );
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}

