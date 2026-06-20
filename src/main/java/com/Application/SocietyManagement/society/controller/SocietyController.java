package com.Application.SocietyManagement.society.controller;

import com.Application.SocietyManagement.society.dto.RegisterSocietyRequest;
import com.Application.SocietyManagement.society.dto.SocietyResponse;
import com.Application.SocietyManagement.society.dto.VerifySocietyRequest;
import com.Application.SocietyManagement.society.enums.SocietyStatus;
import com.Application.SocietyManagement.society.service.SocietyService;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/societies")
@RequiredArgsConstructor
@Tag(name = "Societies",
        description = "Society registration and management")
public class SocietyController {

    private final SocietyService societyService;

    @Operation(summary = "Register a new society",
            description = "Public. Submits society for verification. " +
                    "Upload registration certificate as multipart.")
    @PostMapping(value = "/register",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> register(
            @RequestPart("data") @Valid RegisterSocietyRequest request,
            @RequestPart(value = "document", required = false)
            MultipartFile document) throws IOException {
        return ResponseEntity.accepted()
                .body(societyService.register(request, document));
    }

    @Operation(summary = "Get society by join code",
            description = "Public. Returns society info for join link.")
    @GetMapping("/join/{joinCode}")
    public ResponseEntity<SocietyResponse> getByJoinCode(
            @PathVariable String joinCode) {
        return ResponseEntity.ok(
                societyService.getByJoinCode(joinCode));
    }

    @Operation(summary = "Get my society",
            description = "Returns current user's society details.")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SocietyResponse> getMySociety() {
        return ResponseEntity.ok(societyService.getMySociety());
    }

    @Operation(summary = "Verify society (Platform Admin only)",
            description = "Approve or reject a society registration.")
    @PatchMapping("/{id}/verify")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<SocietyResponse> verify(
            @PathVariable String id,
            @RequestBody @Valid VerifySocietyRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                societyService.verify(id, request, currentUser.getId()));
    }

    @Operation(summary = "List all societies (Platform Admin only)")
    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<PagedResponse<SocietyResponse>> listAll(
            @RequestParam(required = false) SocietyStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                societyService.listAll(status, page, size));
    }

    @Operation(summary = "Get society document URL (Platform Admin only)")
    @GetMapping("/{id}/document")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<Map<String, String>> getDocumentUrl(
            @PathVariable String id) {
        return ResponseEntity.ok(Map.of(
                "url", societyService.getDocumentUrl(id)));
    }
}