package com.Application.SocietyManagement.core.security;

import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.service.CustomUserDetailsService;
import com.Application.SocietyManagement.users.service.JwtService;
import org.springframework.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!jwtService.isValid(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Invalid or expired token\",\"path\":\"" + request.getRequestURI() + "\"}");
            return;
        }

        String email = jwtService.extractEmail(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);

        if (userDetails != null) {
            if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"Account is inactive, pending approval, or blocked\",\"path\":\"" + request.getRequestURI() + "\"}");
                return;
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        String societyId = jwtService.extractSocietyId(token);
        if (societyId == null && userDetails instanceof User user) {
            societyId = user.getSocietyId();
        }
        TenantContext.setSocietyId(societyId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
