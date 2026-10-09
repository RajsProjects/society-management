package com.Application.SocietyManagement.core.logging;

import org.springframework.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final java.util.regex.Pattern TRACE_ID_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]{1,64}$");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String incomingTraceId = request.getHeader("X-Trace-Id");
        if (incomingTraceId == null || incomingTraceId.isBlank()) {
            incomingTraceId = request.getHeader("X-Request-Id");
        }
        String traceId;
        if (incomingTraceId != null && TRACE_ID_PATTERN.matcher(incomingTraceId.trim()).matches()) {
            traceId = incomingTraceId.trim();
        } else {
            traceId = UUID.randomUUID().toString().substring(0, 8);
        }

        long startTime = System.currentTimeMillis();

        MDC.put("traceId", traceId);
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());

        response.setHeader("X-Trace-Id", traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            log.info("{} {} → {} ({}ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    duration);
            MDC.clear();
        }
    }
}
