package com.Application.SocietyManagement.core.security;

import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.society.entity.Society;
import com.Application.SocietyManagement.society.enums.SubscriptionStatus;
import com.Application.SocietyManagement.society.repository.SocietyRepository;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class SubscriptionEnforcementFilter extends OncePerRequestFilter {

    private final SocietyRepository societyRepository;

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    // Paths a locked-out society must still be able to hit so it can pay to reactivate
    private static final List<String> ALLOWED_PREFIXES = List.of(
            "/api/v1/auth",
            "/api/v1/subscriptions",
            "/api/v1/webhooks",
            "/api/v1/societies/register",
            "/api/v1/societies/join",
            "/swagger-ui",
            "/v3/api-docs",
            "/actuator"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull FilterChain filterChain)
            throws ServletException, IOException {

        if (!WRITE_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (ALLOWED_PREFIXES.stream().anyMatch(path::startsWith)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_PLATFORM_ADMIN"))) {
            filterChain.doFilter(request, response); // platform admins aren't bound by a society subscription
            return;
        }

        String societyId = TenantContext.getSocietyId();
        if (societyId == null) {
            filterChain.doFilter(request, response); // no tenant context — let auth layer reject if needed
            return;
        }

        Society society = societyRepository.findById(societyId).orElse(null);
        if (society != null &&
                (society.getSubscriptionStatus() == SubscriptionStatus.EXPIRED
                        || society.getSubscriptionStatus() == SubscriptionStatus.PAST_DUE)) {
            response.setStatus(402); // Payment Required
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Subscription expired\",\"message\":\"Your society's subscription has expired. "
                            + "Existing data remains viewable, but renew to make changes.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}