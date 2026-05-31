package com.Application.SocietyManagement.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStats {
    private long totalResidents;
    private long pendingApprovals;
    private long totalFlats;
    private long occupiedFlats;
    private long openComplaints;
    private long inProgressComplaints;
    private long resolvedComplaints;
    private long totalBills;
    private long paidBills;
    private long overdueBills;
    private long openIssues;
    private long resolvedIssues;
}