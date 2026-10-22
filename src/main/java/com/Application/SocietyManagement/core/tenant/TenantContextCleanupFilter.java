package com.Application.SocietyManagement.core.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Top-level cleanup filter that guarantees TenantContext is reset at the start
 * and strictly cleared at the completion of every HTTP request, regardless of
 * authentication status, exceptions, early exits, or virtual thread recycling.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TenantContextCleanupFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // Defensive cleanup to eliminate any stale context from recycled worker threads
            TenantContext.clear();
            filterChain.doFilter(request, response);
        } finally {
            // Unconditional cleanup ensuring no tenant isolation cross-request leakage
            TenantContext.clear();
        }
    }
}
