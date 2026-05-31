package com.Application.SocietyManagement.users.controller;

import com.Application.SocietyManagement.users.dto.*;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.service.AuthService;
import com.Application.SocietyManagement.users.service.InviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final InviteService inviteService;

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

    @Operation(summary = "Invite resident",
            description = "Sends email invite to a new resident. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Invite sent"),
            @ApiResponse(responseCode = "409", description = "Email already registered or invited")
    })
    @PostMapping("/invite")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> invite(
            @RequestBody @Valid InviteRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                inviteService.invite(request, currentUser.getId()));
    }

    @Operation(summary = "Accept invite",
            description = "Creates account using invite token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account created"),
            @ApiResponse(responseCode = "404", description = "Invalid token"),
            @ApiResponse(responseCode = "409", description = "Token already used"),
            @ApiResponse(responseCode = "410", description = "Token expired")
    })
    @PostMapping("/accept-invite")
    public ResponseEntity<AuthResponse> acceptInvite(
            @RequestBody @Valid AcceptInviteRequest request) {
        return ResponseEntity.ok(inviteService.acceptInvite(request));
    }
}