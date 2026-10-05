package com.Application.SocietyManagement.dashboard.controller;

import com.Application.SocietyManagement.complaint.enums.ComplaintStatus;
import com.Application.SocietyManagement.complaint.repository.ComplaintRepository;
import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.finance.enums.BillStatus;
import com.Application.SocietyManagement.finance.repository.MaintenanceBillRepository;
import com.Application.SocietyManagement.flat.repository.FlatRepository;
import com.Application.SocietyManagement.issue.enums.IssueStatus;
import com.Application.SocietyManagement.issue.repository.IssueRepository;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardController")
class DashboardControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private FlatRepository flatRepository;

    @Mock
    private MaintenanceBillRepository billRepository;

    @Mock
    private IssueRepository issueRepository;

    @InjectMocks
    private DashboardController dashboardController;

    private MockMvc mockMvc;
    private static final String SOCIETY_ID = "society-test-999";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
        TenantContext.setSocietyId(SOCIETY_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("GET /api/v1/dashboard/stats returns all aggregated society metrics")
    void getStats_success_returnsAggregatedMetrics() throws Exception {
        when(userRepository.countBySocietyIdAndRole(SOCIETY_ID, Roles.RESIDENT)).thenReturn(55L);
        when(userRepository.countBySocietyIdAndStatus(SOCIETY_ID, Status.PENDING)).thenReturn(4L);
        when(flatRepository.countBySocietyId(SOCIETY_ID)).thenReturn(100L);
        when(flatRepository.countBySocietyIdAndOccupied(SOCIETY_ID, true)).thenReturn(85L);
        when(complaintRepository.countBySocietyIdAndStatus(SOCIETY_ID, ComplaintStatus.OPEN)).thenReturn(3L);
        when(complaintRepository.countBySocietyIdAndStatus(SOCIETY_ID, ComplaintStatus.IN_PROGRESS)).thenReturn(2L);
        when(complaintRepository.countBySocietyIdAndStatus(SOCIETY_ID, ComplaintStatus.RESOLVED)).thenReturn(25L);
        when(billRepository.countBySocietyId(SOCIETY_ID)).thenReturn(85L);
        when(billRepository.countBySocietyIdAndStatus(SOCIETY_ID, BillStatus.PAID)).thenReturn(75L);
        when(billRepository.countBySocietyIdAndStatus(SOCIETY_ID, BillStatus.OVERDUE)).thenReturn(10L);
        when(issueRepository.countBySocietyIdAndStatus(SOCIETY_ID, IssueStatus.OPEN)).thenReturn(2L);
        when(issueRepository.countBySocietyIdAndStatus(SOCIETY_ID, IssueStatus.RESOLVED)).thenReturn(18L);

        mockMvc.perform(get("/api/v1/dashboard/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalResidents").value(55))
                .andExpect(jsonPath("$.pendingApprovals").value(4))
                .andExpect(jsonPath("$.totalFlats").value(100))
                .andExpect(jsonPath("$.occupiedFlats").value(85))
                .andExpect(jsonPath("$.openComplaints").value(3))
                .andExpect(jsonPath("$.inProgressComplaints").value(2))
                .andExpect(jsonPath("$.resolvedComplaints").value(25))
                .andExpect(jsonPath("$.totalBills").value(85))
                .andExpect(jsonPath("$.paidBills").value(75))
                .andExpect(jsonPath("$.overdueBills").value(10))
                .andExpect(jsonPath("$.openIssues").value(2))
                .andExpect(jsonPath("$.resolvedIssues").value(18));
    }
}
