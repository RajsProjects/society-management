package com.Application.SocietyManagement.complaint.controller;

import com.Application.SocietyManagement.complaint.dto.ComplaintResponse;
import com.Application.SocietyManagement.complaint.dto.CreateComplaintRequest;
import com.Application.SocietyManagement.complaint.dto.UpdateComplaintRequest;
import com.Application.SocietyManagement.complaint.enums.ComplaintCategory;
import com.Application.SocietyManagement.complaint.enums.ComplaintStatus;
import com.Application.SocietyManagement.complaint.service.ComplaintService;
import com.Application.SocietyManagement.users.dto.PagedResponse;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/complaints")
@RequiredArgsConstructor
@Tag(name = "Complaints", description = "Complaint management and tracking")
public class ComplaintController {

    private final ComplaintService complaintService;

    @Operation(summary = "Create complaint",
            description = "Resident creates a new complaint.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Complaint created"),
            @ApiResponse(responseCode = "400", description = "Validation failed")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('RESIDENT', 'ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ComplaintResponse> create(
            @RequestBody @Valid CreateComplaintRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(complaintService.create(request, currentUser));
    }

    @Operation(summary = "List complaints",
            description = "Admin sees all. Resident sees own only.")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PagedResponse<ComplaintResponse>> getAll(
            @RequestParam(required = false) ComplaintStatus status,
            @RequestParam(required = false) ComplaintCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                complaintService.getComplaints(
                        status, category, page, size, currentUser));
    }

    @Operation(summary = "Get complaint by ID")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComplaintResponse> getById(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                complaintService.getById(id, currentUser));
    }

    @Operation(summary = "Update complaint status",
            description = "Admin updates status and adds note.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "404", description = "Complaint not found")
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable String id,
            @RequestBody @Valid UpdateComplaintRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(
                complaintService.updateStatus(id, request, currentUser));
    }

    @Operation(summary = "Delete complaint",
            description = "Only OPEN complaints can be deleted by owner.")
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        complaintService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get complaint stats",
            description = "Dashboard stats. Admin only.")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(complaintService.getStats());
    }
}