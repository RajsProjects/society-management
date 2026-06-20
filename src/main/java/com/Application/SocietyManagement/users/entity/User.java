package com.Application.SocietyManagement.users.entity;

import com.Application.SocietyManagement.core.common.BaseEntity;
import com.Application.SocietyManagement.users.enums.PlatformRole;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Document(collection = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity implements UserDetails {
    private String societyId;
    private String email;
    private String phone;                 // required
    private String passwordHash;
    private String firstName;
    private String lastName;

    @Builder.Default
    private PlatformRole platformRole = PlatformRole.SOCIETY_USER;    // PLATFORM_ADMIN, SOCIETY_USER

    private Roles role;                   // SUPER_ADMIN, ADMIN, ACCOUNTANT,
    // SECURITY, RESIDENT
    private Status status;                // PENDING, ACTIVE, INACTIVE, BLOCKED
    private String flatId;                // which flat they belong to
    private boolean isOwner;             // owner vs tenant
    private String profilePhotoUrl;       // S3 URL
    private Instant lastLoginAt;
    private String fcmToken;              // Android push notifications


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        // society role
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        // platform role
        if (platformRole == PlatformRole.PLATFORM_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return "";
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return status != Status.INACTIVE && status != Status.BLOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return status == Status.ACTIVE; }
}