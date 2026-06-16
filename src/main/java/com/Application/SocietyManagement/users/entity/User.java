package com.Application.SocietyManagement.users.entity;

import com.Application.SocietyManagement.core.common.BaseEntity;
import com.Application.SocietyManagement.users.enums.PlatformRole;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Document(collection = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
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



}