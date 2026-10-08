package com.Application.SocietyManagement.users.controller;

import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.dto.UpdateStatusRequest;
import com.Application.SocietyManagement.users.dto.UserSummarydto;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.Application.SocietyManagement.users.dto.UserResponseDto;
import com.Application.SocietyManagement.users.entity.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User management", description = "Operations for user profiles and resident accounts")
public class AdminController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Operation(
            summary = "Get current user profile",
            description = "Returns profile details of the currently authenticated user."
    )
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(userService.getCurrentUser(currentUser));
    }

    @Operation(
            summary = "Change password",
            description = "Updates password for the currently authenticated user."
    )
    @PostMapping("/change-password")
    public ResponseEntity<java.util.Map<String, String>> changePassword(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid com.Application.SocietyManagement.users.dto.ChangePasswordRequest request) {
        userService.changePassword(currentUser, request, passwordEncoder);
        return ResponseEntity.ok(java.util.Map.of("message", "Password changed successfully"));
    }

    @Operation(
            summary = "List all users",
            description = "Returns a paginated list of users. Optionally filter by status and role. Admin only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid status or role filter"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<UserSummarydto>> getUsers(
            @Parameter(description = "Page number, 0-indexed")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Filter by status: PENDING, ACTIVE, INACTIVE")
            @RequestParam(required = false) Status status,
            @Parameter(description = "Filter by role: ADMIN, RESIDENT")
            @RequestParam(required = false) Roles role) {
        return ResponseEntity.ok(userService.getUsers(page, size, status, role));
    }

    @Operation(
            summary = "Update user status",
            description = "Updates a resident's status. Cannot modify another admin's status. Admin only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "400", description = "Invalid status value"),
            @ApiResponse(responseCode = "403", description = "Cannot modify another admin"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserSummarydto> updateStatus(
            @Parameter(description = "User ID") @PathVariable String userId,
            @RequestBody @Valid UpdateStatusRequest request) {
        return ResponseEntity.ok(userService.updateStatus(userId, request.getStatus()));
    }
}