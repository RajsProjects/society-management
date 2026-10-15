package com.Application.SocietyManagement.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String type;         // "Bearer"
    private String role;         // SUPER_ADMIN / ADMIN / RESIDENT (tenant role)
    private String platformRole; // PLATFORM_ADMIN / SOCIETY_USER (platform scope)
    private String firstName;
    private String lastName;
    private String societyId;
    private String message;
}