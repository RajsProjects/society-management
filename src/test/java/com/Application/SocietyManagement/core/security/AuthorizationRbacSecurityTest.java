package com.Application.SocietyManagement.core.security;

import com.Application.SocietyManagement.communication.entity.SocietyGroup;
import com.Application.SocietyManagement.communication.repository.SocietyGroupRepository;
import com.Application.SocietyManagement.communication.service.SocietyGroupService;
import com.Application.SocietyManagement.complaint.entity.Complaint;
import com.Application.SocietyManagement.complaint.enums.ComplaintCategory;
import com.Application.SocietyManagement.complaint.enums.ComplaintStatus;
import com.Application.SocietyManagement.complaint.repository.ComplaintRepository;
import com.Application.SocietyManagement.complaint.service.ComplaintService;
import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.finance.dto.PayBillRequest;
import com.Application.SocietyManagement.finance.entity.MaintenanceBill;
import com.Application.SocietyManagement.finance.enums.BillStatus;
import com.Application.SocietyManagement.finance.repository.MaintenanceBillRepository;
import com.Application.SocietyManagement.finance.service.MaintenanceBillService;
import com.Application.SocietyManagement.users.dto.UpdateResidentRequest;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.PlatformRole;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import com.Application.SocietyManagement.users.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security: Authorization, RBAC & IDOR/BOLA Protection Test Suite")
class AuthorizationRbacSecurityTest {

        private static final String SOCIETY_ALPHA = "soc-alpha";
        private static final String SOCIETY_BETA = "soc-beta";

        @Mock
        private ComplaintRepository complaintRepository;

        @Mock
        private MaintenanceBillRepository billRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private SocietyGroupRepository groupRepository;

        private ComplaintService complaintService;
        private MaintenanceBillService billService;
        private UserService userService;
        private SocietyGroupService groupService;

        private User adminAlpha;
        private User superAdminAlpha;
        private User resident1Alpha;
        private User resident2Alpha;
        private User residentBeta;

        @BeforeEach
        void setUp() {
                TenantContext.setSocietyId(SOCIETY_ALPHA);
                complaintService = new ComplaintService(complaintRepository);
                billService = new MaintenanceBillService(billRepository, userRepository, null);
                userService = new UserService(userRepository, null, null);
                groupService = new SocietyGroupService(groupRepository, null, userRepository, null);

                adminAlpha = User.builder()
                                .email("admin@alpha.com")
                                .role(Roles.ADMIN)
                                .platformRole(PlatformRole.SOCIETY_USER)
                                .status(Status.ACTIVE)
                                .societyId(SOCIETY_ALPHA)
                                .build();
                adminAlpha.setId("admin-alpha-id");

                superAdminAlpha = User.builder()
                                .email("superadmin@alpha.com")
                                .role(Roles.SUPER_ADMIN)
                                .platformRole(PlatformRole.SOCIETY_USER)
                                .status(Status.ACTIVE)
                                .societyId(SOCIETY_ALPHA)
                                .build();
                superAdminAlpha.setId("superadmin-alpha-id");

                resident1Alpha = User.builder()
                                .email("resident1@alpha.com")
                                .role(Roles.RESIDENT)
                                .platformRole(PlatformRole.SOCIETY_USER)
                                .status(Status.ACTIVE)
                                .societyId(SOCIETY_ALPHA)
                                .build();
                resident1Alpha.setId("resident1-alpha-id");

                resident2Alpha = User.builder()
                                .email("resident2@alpha.com")
                                .role(Roles.RESIDENT)
                                .platformRole(PlatformRole.SOCIETY_USER)
                                .status(Status.ACTIVE)
                                .societyId(SOCIETY_ALPHA)
                                .build();
                resident2Alpha.setId("resident2-alpha-id");

                residentBeta = User.builder()
                                .email("resident@beta.com")
                                .role(Roles.RESIDENT)
                                .platformRole(PlatformRole.SOCIETY_USER)
                                .status(Status.ACTIVE)
                                .societyId(SOCIETY_BETA)
                                .build();
                residentBeta.setId("resident-beta-id");
        }

        @AfterEach
        void tearDown() {
                TenantContext.clear();
        }

        // ── 1. Complaint IDOR & Cross-Tenant Access Tests ──

        @Test
        @DisplayName("IDOR Prevention: Resident cannot view another resident's private complaint")
        void residentCannotViewOtherResidentsComplaint_IDOR() {
                TenantContext.setSocietyId(SOCIETY_ALPHA);

                Complaint complaintOfResident2 = Complaint.builder()
                                .societyId(SOCIETY_ALPHA)
                                .residentId("resident2-alpha-id")
                                .title("Leaky pipe")
                                .category(ComplaintCategory.PLUMBING)
                                .status(ComplaintStatus.OPEN)
                                .build();
                complaintOfResident2.setId("comp-2");

                when(complaintRepository.findByIdAndSocietyId("comp-2", SOCIETY_ALPHA))
                                .thenReturn(Optional.of(complaintOfResident2));

                assertThatThrownBy(() -> complaintService.getById("comp-2", resident1Alpha))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                                        assertThat(rse.getReason()).contains("Access denied");
                                });
        }

        @Test
        @DisplayName("IDOR Prevention: Resident cannot delete another resident's complaint")
        void residentCannotDeleteOtherResidentsComplaint_IDOR() {
                TenantContext.setSocietyId(SOCIETY_ALPHA);

                Complaint complaintOfResident2 = Complaint.builder()
                                .societyId(SOCIETY_ALPHA)
                                .residentId("resident2-alpha-id")
                                .title("Noise issue")
                                .category(ComplaintCategory.NOISE)
                                .status(ComplaintStatus.OPEN)
                                .build();
                complaintOfResident2.setId("comp-2");

                when(complaintRepository.findByIdAndSocietyId("comp-2", SOCIETY_ALPHA))
                                .thenReturn(Optional.of(complaintOfResident2));

                assertThatThrownBy(() -> complaintService.delete("comp-2", resident1Alpha))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                                                .isEqualTo(HttpStatus.FORBIDDEN));

                verify(complaintRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Cross-Tenant Isolation: User cannot access complaint belonging to different society")
        void userCannotAccessComplaintFromOtherSociety_CrossTenant() {
                TenantContext.setSocietyId(SOCIETY_ALPHA);

                when(complaintRepository.findByIdAndSocietyId("comp-beta-1", SOCIETY_ALPHA))
                                .thenReturn(Optional.empty());

                assertThatThrownBy(() -> complaintService.getById("comp-beta-1", resident1Alpha))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                                                .isEqualTo(HttpStatus.NOT_FOUND));
        }

        // ── 2. Maintenance Bill IDOR & BOLA Tests ──

        @Test
        @DisplayName("IDOR Prevention: Resident cannot retrieve another resident's maintenance bill")
        void residentCannotViewOtherResidentsBill_IDOR() {
                TenantContext.setSocietyId(SOCIETY_ALPHA);

                MaintenanceBill billResident2 = MaintenanceBill.builder()
                                .societyId(SOCIETY_ALPHA)
                                .userId("resident2-alpha-id")
                                .amount(BigDecimal.valueOf(2500))
                                .status(BillStatus.PENDING)
                                .build();
                billResident2.setId("bill-res2");

                when(billRepository.findByIdAndSocietyId("bill-res2", SOCIETY_ALPHA))
                                .thenReturn(Optional.of(billResident2));

                assertThatThrownBy(() -> billService.getBillById("bill-res2", resident1Alpha))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                                                .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("IDOR Prevention: Resident cannot submit payment against another resident's bill")
        void residentCannotPayOtherResidentsBill_IDOR() {
                TenantContext.setSocietyId(SOCIETY_ALPHA);

                MaintenanceBill billResident2 = MaintenanceBill.builder()
                                .societyId(SOCIETY_ALPHA)
                                .userId("resident2-alpha-id")
                                .amount(BigDecimal.valueOf(2500))
                                .status(BillStatus.PENDING)
                                .build();
                billResident2.setId("bill-res2");

                when(billRepository.findByIdAndSocietyId("bill-res2", SOCIETY_ALPHA))
                                .thenReturn(Optional.of(billResident2));

                PayBillRequest payReq = new PayBillRequest();
                payReq.setAmount(BigDecimal.valueOf(2500));
                payReq.setUpiTransactionId("UPI-123456789012");

                assertThatThrownBy(() -> billService.payBill("bill-res2", resident1Alpha, payReq))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                                                .isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("BOLA Prevention: Resident querying bills is strictly limited to their own user records")
        void residentQueryingBills_restrictedToOwnUserRecords() {
                when(billRepository.findByUserIdAndSocietyId(eq("resident1-alpha-id"), eq(SOCIETY_ALPHA), any()))
                                .thenReturn(org.springframework.data.domain.Page.empty());

                billService.getBills(resident1Alpha, null, 0, 20);

                verify(billRepository).findByUserIdAndSocietyId(eq("resident1-alpha-id"), eq(SOCIETY_ALPHA), any());
                verify(billRepository, never()).findBySocietyId(any(), any());
        }

        // ── 3. Privilege Escalation & Admin Isolation Tests ──

        @Test
        @DisplayName("Privilege Escalation Prevention: ADMIN cannot modify or hijack SUPER_ADMIN account")
        void adminCannotModifySuperAdmin_PrivilegeEscalation() {
                when(userRepository.findByIdAndSocietyId("superadmin-alpha-id", SOCIETY_ALPHA))
                                .thenReturn(Optional.of(superAdminAlpha));

                UpdateResidentRequest req = UpdateResidentRequest.builder()
                                .firstName("Hijacked")
                                .lastName("Account")
                                .build();

                assertThatThrownBy(() -> userService.updateResident("superadmin-alpha-id", req, adminAlpha))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                                        assertThat(rse.getReason())
                                                        .contains("Cannot modify users of equal or higher privilege");
                                });
        }

        @Test
        @DisplayName("Privilege Escalation Prevention: ADMIN cannot modify lateral ADMIN accounts")
        void adminCannotModifyLateralAdmin_PrivilegeEscalation() {
                User anotherAdmin = User.builder()
                                .email("admin2@alpha.com")
                                .role(Roles.ADMIN)
                                .platformRole(PlatformRole.SOCIETY_USER)
                                .status(Status.ACTIVE)
                                .societyId(SOCIETY_ALPHA)
                                .build();
                anotherAdmin.setId("admin2-alpha-id");

                when(userRepository.findByIdAndSocietyId("admin2-alpha-id", SOCIETY_ALPHA))
                                .thenReturn(Optional.of(anotherAdmin));

                UpdateResidentRequest req = UpdateResidentRequest.builder()
                                .firstName("Lateral")
                                .lastName("Admin")
                                .build();

                assertThatThrownBy(() -> userService.updateResident("admin2-alpha-id", req, adminAlpha))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                                                .isEqualTo(HttpStatus.FORBIDDEN));
        }

        // ── 4. Cross-Society Group Isolation Tests ──

        @Test
        @DisplayName("Cross-Tenant Isolation: Resident cannot view community group of another society")
        void residentCannotViewOtherSocietyGroup_CrossTenant() {
                SocietyGroup betaGroup = SocietyGroup.builder()
                                .societyId(SOCIETY_BETA)
                                .name("Beta Community Group")
                                .memberUserIds(Set.of("resident-beta-id"))
                                .build();
                betaGroup.setId("grp-beta");

                when(groupRepository.findById("grp-beta")).thenReturn(Optional.of(betaGroup));

                assertThatThrownBy(() -> groupService.getGroupById("grp-beta", resident1Alpha))
                                .isInstanceOf(ResponseStatusException.class)
                                .satisfies(ex -> {
                                        ResponseStatusException rse = (ResponseStatusException) ex;
                                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                                        assertThat(rse.getReason()).contains("Access denied to other society's group");
                                });
        }
}
