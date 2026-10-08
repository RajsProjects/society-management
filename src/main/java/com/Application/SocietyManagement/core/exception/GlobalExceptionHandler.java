package com.Application.SocietyManagement.core.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 - validation failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", 400);
        body.put("error", "Validation failed");
        body.put("details", fieldErrors);
        body.put("path", request.getRequestURI());
        body.put("traceId", MDC.get("traceId"));

        log.warn("Validation failed for {}: {}", request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(body);
    }

    // 400 - invalid enum in query param
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleEnumMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        log.warn("Invalid parameter value '{}' for field '{}'", ex.getValue(), ex.getName());
        return ResponseEntity.badRequest().body(error(400,
                "Invalid value: " + ex.getValue(), request));
    }

    // 400 - malformed JSON or unreadable request body / invalid body enum
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request) {
        String msg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.warn("Malformed JSON request at {}: {}", request.getRequestURI(), msg);
        return ResponseEntity.badRequest().body(error(400,
                "Malformed JSON request: " + msg, request));
    }

    // 400 - missing required header
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex,
            HttpServletRequest request) {
        log.warn("Missing required header '{}' at {}", ex.getHeaderName(), request.getRequestURI());
        return ResponseEntity.badRequest().body(error(400,
                "Required header missing: " + ex.getHeaderName(), request));
    }

    // 413 - upload size exceeded
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        log.warn("Payload too large at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(error(413, "Uploaded file exceeds maximum allowed size (5MB)", request));
    }

    // 400 - illegal argument exception
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {
        log.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(error(400, ex.getMessage(), request));
    }

    // 404 - no resource found (static route or non-existent endpoint)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(
            NoResourceFoundException ex,
            HttpServletRequest request) {
        log.warn("Resource not found at {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(404,
                "Endpoint not found: " + request.getRequestURI(), request));
    }

    // 403 - access denied (Spring Security)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request) {
        log.warn("Access denied to {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(403, "Access denied", request));
    }

    // 403 - custom forbidden
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex,
            HttpServletRequest request) {
        log.warn("Forbidden: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(error(403, ex.getMessage(), request));
    }

    // 404 - not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        log.warn("Not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error(404, ex.getMessage(), request));
    }

    // 409 - conflict
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            ConflictException ex,
            HttpServletRequest request) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(409, ex.getMessage(), request));
    }

    // ResponseStatusException (used across services)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException ex,
            HttpServletRequest request) {
        log.warn("ResponseStatusException {}: {}", ex.getStatusCode(), ex.getReason());
        return ResponseEntity.status(ex.getStatusCode())
                .body(error(ex.getStatusCode().value(), ex.getReason(), request));
    }

    // 500 - catch-all
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        return ResponseEntity.internalServerError()
                .body(error(500, "An unexpected error occurred", request));
    }

    private ErrorResponse error(int status, String message, HttpServletRequest request) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status)
                .error(message)
                .path(request.getRequestURI())
                .traceId(MDC.get("traceId"))
                .build();
    }
}