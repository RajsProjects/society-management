package com.Application.SocietyManagement.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String type;   // "Bearer"
    private String role;// ADMIN / RESIDENT
    private String message;
}