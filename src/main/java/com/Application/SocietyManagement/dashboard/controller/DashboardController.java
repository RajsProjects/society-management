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

        DashboardStats stats = DashboardStats.builder()
                .totalResidents(userRepository
                        .countBySocietyIdAndRole(societyId, Roles.RESIDENT))
                .pendingApprovals(userRepository
                        .countBySocietyIdAndStatus(societyId, Status.PENDING))
                .totalFlats(flatRepository
                        .countBySocietyId(societyId))
                .occupiedFlats(flatRepository
                        .countBySocietyIdAndOccupied(societyId, true))
                .openComplaints(complaintRepository
                        .countBySocietyIdAndStatus(societyId,
                                ComplaintStatus.OPEN))
                .inProgressComplaints(complaintRepository
                        .countBySocietyIdAndStatus(societyId,
                                ComplaintStatus.IN_PROGRESS))
                .resolvedComplaints(complaintRepository
                        .countBySocietyIdAndStatus(societyId,
                                ComplaintStatus.RESOLVED))
                .totalBills(billRepository
                        .countBySocietyId(societyId))
                .paidBills(billRepository
                        .countBySocietyIdAndStatus(societyId, BillStatus.PAID))
                .overdueBills(billRepository
                        .countBySocietyIdAndStatus(societyId,
                                BillStatus.OVERDUE))
                .openIssues(issueRepository
                        .countBySocietyIdAndStatus(societyId,
                                IssueStatus.OPEN))
                .resolvedIssues(issueRepository
                        .countBySocietyIdAndStatus(societyId,
                                IssueStatus.RESOLVED))
                .build();

        return ResponseEntity.ok(stats);
    }
}