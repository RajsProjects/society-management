package com.Application.SocietyManagement.communication.controller;

import com.Application.SocietyManagement.communication.dto.AnnouncementRequest;
import com.Application.SocietyManagement.communication.dto.AnnouncementResponse;
import com.Application.SocietyManagement.communication.enums.AnnouncementType;
import com.Application.SocietyManagement.communication.service.AnnouncementService;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
@Tag(name = "Announcements", description = "Community announcements and alerts")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "Create announcement", description = "Creates a new announcement. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Announcement created"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnnouncementResponse> create(
            @RequestBody @Valid AnnouncementRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(announcementService.create(request, currentUser.getId()));
    }

    @Operation(summary = "List announcements", description = "Returns paginated announcements, newest first. Optionally filter by type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Announcements retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid type filter")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<PagedResponse<AnnouncementResponse>> getAll(
            @Parameter(description = "Filter by type: GENERAL, MAINTENANCE, EMERGENCY")
            @RequestParam(required = false) AnnouncementType type,
            @Parameter(description = "Page number, 0-indexed")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(announcementService.getAll(type, page, size));
    }

    @Operation(summary = "Get announcement by ID", description = "Returns a single announcement.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Announcement found"),
            @ApiResponse(responseCode = "404", description = "Announcement not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<AnnouncementResponse> getById(
            @Parameter(description = "Announcement ID") @PathVariable String id) {
        return ResponseEntity.ok(announcementService.getById(id));
    }

    @Operation(summary = "Update announcement", description = "Updates an existing announcement. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Announcement updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed"),
            @ApiResponse(responseCode = "404", description = "Announcement not found")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AnnouncementResponse> update(
            @Parameter(description = "Announcement ID") @PathVariable String id,
            @RequestBody @Valid AnnouncementRequest request) {
        return ResponseEntity.ok(announcementService.update(id, request));
    }

    @Operation(summary = "Delete announcement", description = "Deletes an announcement. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Announcement deleted"),
            @ApiResponse(responseCode = "404", description = "Announcement not found")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Announcement ID") @PathVariable String id) {
        announcementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}