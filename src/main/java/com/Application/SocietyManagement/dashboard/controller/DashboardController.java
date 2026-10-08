package com.Application.SocietyManagement.dashboard.controller;

import com.Application.SocietyManagement.complaint.enums.ComplaintStatus;
import com.Application.SocietyManagement.complaint.repository.ComplaintRepository;
import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.dashboard.dto.DashboardStats;
import com.Application.SocietyManagement.finance.enums.BillStatus;
import com.Application.SocietyManagement.finance.repository.MaintenanceBillRepository;
import com.Application.SocietyManagement.flat.repository.FlatRepository;
import com.Application.SocietyManagement.issue.enums.IssueStatus;
import com.Application.SocietyManagement.issue.repository.IssueRepository;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Admin dashboard statistics")
public class DashboardController {

    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;
    private final FlatRepository flatRepository;
    private final MaintenanceBillRepository billRepository;
    private final IssueRepository issueRepository;

    @Operation(summary = "Get dashboard stats",
            description = "Returns all dashboard statistics. Admin only.")
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public ResponseEntity<DashboardStats> getStats() {
        String societyId = TenantContext.getSocietyId();

        java.util.concurrent.CompletableFuture<Long> totalResidents = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> userRepository.countBySocietyIdAndRole(societyId, Roles.RESIDENT));
        java.util.concurrent.CompletableFuture<Long> pendingApprovals = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> userRepository.countBySocietyIdAndStatus(societyId, Status.PENDING));
        java.util.concurrent.CompletableFuture<Long> totalFlats = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> flatRepository.countBySocietyId(societyId));
        java.util.concurrent.CompletableFuture<Long> occupiedFlats = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> flatRepository.countBySocietyIdAndOccupied(societyId, true));
        java.util.concurrent.CompletableFuture<Long> openComplaints = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> complaintRepository.countBySocietyIdAndStatus(societyId, ComplaintStatus.OPEN));
        java.util.concurrent.CompletableFuture<Long> inProgressComplaints = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> complaintRepository.countBySocietyIdAndStatus(societyId, ComplaintStatus.IN_PROGRESS));
        java.util.concurrent.CompletableFuture<Long> resolvedComplaints = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> complaintRepository.countBySocietyIdAndStatus(societyId, ComplaintStatus.RESOLVED));
        java.util.concurrent.CompletableFuture<Long> totalBills = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> billRepository.countBySocietyId(societyId));
        java.util.concurrent.CompletableFuture<Long> paidBills = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> billRepository.countBySocietyIdAndStatus(societyId, BillStatus.PAID));
        java.util.concurrent.CompletableFuture<Long> overdueBills = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> billRepository.countBySocietyIdAndStatus(societyId, BillStatus.OVERDUE));
        java.util.concurrent.CompletableFuture<Long> openIssues = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> issueRepository.countBySocietyIdAndStatus(societyId, IssueStatus.OPEN));
        java.util.concurrent.CompletableFuture<Long> resolvedIssues = java.util.concurrent.CompletableFuture.supplyAsync(
                () -> issueRepository.countBySocietyIdAndStatus(societyId, IssueStatus.RESOLVED));

        java.util.concurrent.CompletableFuture.allOf(
                totalResidents, pendingApprovals, totalFlats, occupiedFlats,
                openComplaints, inProgressComplaints, resolvedComplaints,
                totalBills, paidBills, overdueBills, openIssues, resolvedIssues
        ).join();

        DashboardStats stats = DashboardStats.builder()
                .totalResidents(totalResidents.join())
                .pendingApprovals(pendingApprovals.join())
                .totalFlats(totalFlats.join())
                .occupiedFlats(occupiedFlats.join())
                .openComplaints(openComplaints.join())
                .inProgressComplaints(inProgressComplaints.join())
                .resolvedComplaints(resolvedComplaints.join())
                .totalBills(totalBills.join())
                .paidBills(paidBills.join())
                .overdueBills(overdueBills.join())
                .openIssues(openIssues.join())
                .resolvedIssues(resolvedIssues.join())
                .build();

        return ResponseEntity.ok(stats);
    }
}