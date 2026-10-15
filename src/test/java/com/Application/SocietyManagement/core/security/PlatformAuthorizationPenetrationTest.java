package com.Application.SocietyManagement.core.security;

import com.Application.SocietyManagement.core.config.SecurityConfig;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.PlatformRole;
import com.Application.SocietyManagement.users.enums.Roles;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Platform Scope & Tenant Isolation Security Audit")
class PlatformAuthorizationPenetrationTest {

    private final SecurityConfig securityConfig = new SecurityConfig(null, null, null, null);
    private final RoleHierarchy roleHierarchy = securityConfig.roleHierarchy();

    @Test
    @DisplayName("Security Audit: PLATFORM_ADMIN authority contains ONLY ROLE_PLATFORM_ADMIN and no tenant roles")
    void platformAdminAuthorities_strictlyIsolated() {
        User platformAdmin = User.builder()
                .email("sumanmandal8955@gmail.com")
                .platformRole(PlatformRole.PLATFORM_ADMIN)
                .role(null) // No tenant role
                .societyId(null) // No society binding
                .build();

        Collection<? extends GrantedAuthority> authorities = platformAdmin.getAuthorities();

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PLATFORM_ADMIN")
                .doesNotContain("ROLE_ADMIN", "ROLE_RESIDENT", "ROLE_SUPER_ADMIN");
    }

    @Test
    @DisplayName("Security Audit: ROLE_PLATFORM_ADMIN does NOT imply tenant roles (ADMIN, RESIDENT)")
    void roleHierarchy_platformAdminDoesNotImplyTenantRoles() {
        Collection<? extends GrantedAuthority> reachable = roleHierarchy.getReachableGrantedAuthorities(
                List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN")));

        assertThat(reachable)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PLATFORM_ADMIN")
                .doesNotContain("ROLE_ADMIN", "ROLE_RESIDENT", "ROLE_SUPER_ADMIN");
    }

    @Test
    @DisplayName("Security Audit: Tenant SUPER_ADMIN implies ADMIN and RESIDENT within society boundary")
    void roleHierarchy_tenantHierarchyRemainsIntact() {
        Collection<? extends GrantedAuthority> reachableSuperAdmin = roleHierarchy.getReachableGrantedAuthorities(
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));

        assertThat(reachableSuperAdmin)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_RESIDENT")
                .doesNotContain("ROLE_PLATFORM_ADMIN");

        Collection<? extends GrantedAuthority> reachableAdmin = roleHierarchy.getReachableGrantedAuthorities(
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThat(reachableAdmin)
                .extracting(GrantedAuthority::getAuthority)
                .contains("ROLE_ADMIN", "ROLE_RESIDENT")
                .doesNotContain("ROLE_PLATFORM_ADMIN", "ROLE_SUPER_ADMIN");
    }

    @Test
    @DisplayName("Security Audit: Tenant RESIDENT cannot reach ADMIN or PLATFORM_ADMIN")
    void roleHierarchy_residentHasNoElevation() {
        Collection<? extends GrantedAuthority> reachableResident = roleHierarchy.getReachableGrantedAuthorities(
                List.of(new SimpleGrantedAuthority("ROLE_RESIDENT")));

        assertThat(reachableResident)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_RESIDENT")
                .doesNotContain("ROLE_ADMIN", "ROLE_SUPER_ADMIN", "ROLE_PLATFORM_ADMIN");
    }
}
