package com.Application.SocietyManagement.issue.controller;

import com.Application.SocietyManagement.issue.dto.*;
import com.Application.SocietyManagement.issue.enums.IssueStatus;
import com.Application.SocietyManagement.issue.service.IssueService;
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
@RequestMapping("/api/v1/issues")
@RequiredArgsConstructor
@Tag(name = "Issues", description = "Community issue reporting and voting")
public class IssueController {

    private final IssueService issueService;

    @Operation(summary = "Create issue", description = "Reports a new community issue. Admin or resident.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Issue created"),
            @ApiResponse(responseCode = "400", description = "Missing title or description")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<IssueResponse> createIssue(
            @RequestBody @Valid IssueRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(issueService.createIssue(request, currentUser.getId()));
    }

    @Operation(summary = "List issues", description = "Returns all issues with vote counts. Optionally filter by status and sort by voteCount or createdAt.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issues retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid status filter")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<PagedResponse<IssueResponse>> getIssues(
            @Parameter(description = "Filter by status: OPEN, IN_PROGRESS, RESOLVED")
            @RequestParam(required = false) IssueStatus status,
            @Parameter(description = "Sort field: createdAt or voteCount")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String direction,
            @Parameter(description = "Page number, 0-indexed")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                issueService.getIssues(status, sortBy, direction, page, size));
    }

    @Operation(summary = "Update issue status", description = "Updates status to OPEN, IN_PROGRESS or RESOLVED. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "403", description = "Not an admin"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
    })
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IssueResponse> updateStatus(
            @Parameter(description = "Issue ID") @PathVariable String id,
            @RequestBody @Valid IssueStatusRequest request) {
        return ResponseEntity.ok(issueService.updateStatus(id, request.getStatus()));
    }

    @Operation(summary = "Update issue priority", description = "Sets priority to LOW, MEDIUM or HIGH. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Priority updated"),
            @ApiResponse(responseCode = "403", description = "Not an admin"),
            @ApiResponse(responseCode = "404", description = "Issue not found")
    })
    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IssueResponse> updatePriority(
            @Parameter(description = "Issue ID") @PathVariable String id,
            @RequestBody @Valid IssuePriorityRequest request) {
        return ResponseEntity.ok(issueService.updatePriority(id, request.getPriority()));
    }

    @Operation(summary = "Vote on issue", description = "Adds a vote to an issue. Cannot vote on own issue or vote twice.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vote recorded"),
            @ApiResponse(responseCode = "400", description = "Cannot vote on own issue"),
            @ApiResponse(responseCode = "404", description = "Issue not found"),
            @ApiResponse(responseCode = "409", description = "Already voted on this issue")
    })
    @PostMapping("/{id}/votes")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<Void> addVote(
            @Parameter(description = "Issue ID") @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        issueService.addVote(id, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Remove vote from issue", description = "Removes the current user's vote from an issue.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vote removed"),
            @ApiResponse(responseCode = "404", description = "Vote not found")
    })
    @DeleteMapping("/{id}/votes")
    @PreAuthorize("hasAnyRole('ADMIN', 'RESIDENT')")
    public ResponseEntity<Void> removeVote(
            @Parameter(description = "Issue ID") @PathVariable String id,
            @AuthenticationPrincipal User currentUser) {
        issueService.removeVote(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }
}