package com.Application.SocietyManagement.society.controller;

import com.Application.SocietyManagement.society.dto.SocietyRegistrationRequest;
import com.Application.SocietyManagement.society.dto.SocietyResponse;
import com.Application.SocietyManagement.society.dto.UpdateSocietyRequest;
import com.Application.SocietyManagement.society.service.SocietyService;
import com.Application.SocietyManagement.users.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/societies")
@RequiredArgsConstructor
@Tag(name = "Society", description = "Society registration and management")
public class SocietyController {

    private final SocietyService societyService;

    @Operation(
            summary = "Register new society",
            description = "Creates a new society and its first admin account. Public endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Society registered successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "409", description = "Society code or email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<SocietyResponse> register(
            @RequestBody @Valid SocietyRegistrationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(societyService.register(request));
    }

    @Operation(
            summary = "Get my society",
            description = "Returns current admin's society details. Admin only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Society found"),
            @ApiResponse(responseCode = "404", description = "Society not found")
    })
    @GetMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SocietyResponse> getMySociety(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                societyService.getBySocietyId(currentUser.getSocietyId()));
    }

    @Operation(
            summary = "Update society details",
            description = "Updates society name and address. Admin only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Society updated"),
            @ApiResponse(responseCode = "404", description = "Society not found")
    })
    @PutMapping("/me")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SocietyResponse> update(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid UpdateSocietyRequest request) {
        return ResponseEntity.ok(
                societyService.update(
                        currentUser.getSocietyId(), request));
    }
}