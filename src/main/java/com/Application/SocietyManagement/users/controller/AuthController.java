package com.Application.SocietyManagement.users.controller;

import com.Application.SocietyManagement.users.dto.AuthResponse;
import com.Application.SocietyManagement.users.dto.LoginRequest;
import com.Application.SocietyManagement.users.dto.SignupRequest;
import com.Application.SocietyManagement.users.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Signup and login endpoints")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Register resident",
            description = "Creates a new resident account with PENDING status. Admin must approve before login is allowed."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registration successful, awaiting approval"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Email already registered")
    })
    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(
            @RequestBody @Valid SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(201)
                .body(Map.of("message", "Registration successful. Pending admin approval."));
    }

    @Operation(
            summary = "Login",
            description = "Authenticates a user and returns a JWT token. Account must be ACTIVE to login."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, JWT returned"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @ApiResponse(responseCode = "403", description = "Account pending approval or inactive")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}