package com.Application.SocietyManagement.society.service;

import com.Application.SocietyManagement.core.service.S3Service;
import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.core.util.JoinCodeGenerator;
import com.Application.SocietyManagement.society.dto.RegisterSocietyRequest;
import com.Application.SocietyManagement.society.dto.SocietyResponse;
import com.Application.SocietyManagement.society.dto.VerifySocietyRequest;
import com.Application.SocietyManagement.society.entity.Society;
import com.Application.SocietyManagement.society.enums.SocietyStatus;
import com.Application.SocietyManagement.society.enums.SubscriptionPlan;
import com.Application.SocietyManagement.society.enums.SubscriptionStatus;
import com.Application.SocietyManagement.society.repository.SocietyRepository;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import com.Application.SocietyManagement.users.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocietyService")
class SocietyServiceTest {

    @Mock
    private SocietyRepository societyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private S3Service s3Service;

    @Mock
    private JoinCodeGenerator joinCodeGenerator;

    @InjectMocks
    private SocietyService societyService;

    private static final String SOCIETY_ID = "society-123";
    private static final String USER_ID = "user-123";
    private static final String JOIN_CODE = "SOC-ABC123";

    private Society society;
    private User adminUser;
    private RegisterSocietyRequest registerRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(societyService, "trialDays", 14);

        society = Society.builder()
                .name("Green Heights")
                .registrationNumber("REG-999")
                .address("123 Palm Street")
                .city("Mumbai")
                .state("Maharashtra")
                .pincode("400001")
                .totalFlats(100)
                .adminEmail("admin@greenheights.com")
                .status(SocietyStatus.PENDING_VERIFICATION)
                .joinCode(JOIN_CODE)
                .societyCode(JOIN_CODE)
                .plan(SubscriptionPlan.TRIAL)
                .subscriptionStatus(SubscriptionStatus.TRIAL)
                .superAdminId(USER_ID)
                .build();
        society.setId(SOCIETY_ID);

        adminUser = User.builder()
                .societyId(SOCIETY_ID)
                .email("admin@greenheights.com")
                .phone("9876543210")
                .firstName("John")
                .lastName("Doe")
                .role(Roles.SUPER_ADMIN)
                .status(Status.PENDING)
                .build();
        adminUser.setId(USER_ID);

        registerRequest = new RegisterSocietyRequest();
        registerRequest.setName("Green Heights");
        registerRequest.setRegistrationNumber("REG-999");
        registerRequest.setAddress("123 Palm Street");
        registerRequest.setCity("Mumbai");
        registerRequest.setState("Maharashtra");
        registerRequest.setPincode("400001");
        registerRequest.setTotalFlats(100);
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("admin@greenheights.com");
        registerRequest.setPhone("9876543210");
        registerRequest.setPassword("secretPassword123");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("register")
    class RegisterTests {

        @Test
        @DisplayName("successful registration with document creates society and super admin")
        void register_success_withDocument() throws IOException {
            when(societyRepository.existsByRegistrationNumber("REG-999")).thenReturn(false);
            when(userRepository.findByEmail("admin@greenheights.com")).thenReturn(Optional.empty());
            when(joinCodeGenerator.generate()).thenReturn(JOIN_CODE);
            when(societyRepository.findByJoinCode(JOIN_CODE)).thenReturn(Optional.empty());

            MockMultipartFile document = new MockMultipartFile(
                    "document", "cert.pdf", "application/pdf", "dummy certificate content".getBytes());
            when(s3Service.uploadFile(any(byte[].class), eq("application/pdf"), eq("society-documents")))
                    .thenReturn("s3/key/cert.pdf");

            when(societyRepository.save(any(Society.class))).thenAnswer(invocation -> {
                Society s = invocation.getArgument(0);
                if (s.getId() == null) {
                    s.setId(SOCIETY_ID);
                }
                return s;
            });

            when(passwordEncoder.encode("secretPassword123")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(USER_ID);
                return u;
            });

            Map<String, String> response = societyService.register(registerRequest, document);

            assertThat(response).isNotNull();
            assertThat(response.get("societyId")).isEqualTo(SOCIETY_ID);
            assertThat(response.get("status")).isEqualTo("PENDING_VERIFICATION");

            verify(s3Service).uploadFile(any(), eq("application/pdf"), eq("society-documents"));
            verify(societyRepository, atLeastOnce()).save(any(Society.class));
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("successful registration without document leaves documentUrl null")
        void register_success_withoutDocument() throws IOException {
            when(societyRepository.existsByRegistrationNumber("REG-999")).thenReturn(false);
            when(userRepository.findByEmail("admin@greenheights.com")).thenReturn(Optional.empty());
            when(joinCodeGenerator.generate()).thenReturn(JOIN_CODE);
            when(societyRepository.findByJoinCode(JOIN_CODE)).thenReturn(Optional.empty());

            when(societyRepository.save(any(Society.class))).thenAnswer(invocation -> {
                Society s = invocation.getArgument(0);
                if (s.getId() == null) s.setId(SOCIETY_ID);
                return s;
            });
            when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User u = invocation.getArgument(0);
                u.setId(USER_ID);
                return u;
            });

            Map<String, String> response = societyService.register(registerRequest, null);

            assertThat(response.get("societyId")).isEqualTo(SOCIETY_ID);
            verify(s3Service, never()).uploadFile(any(), any(), any());
        }

        @Test
        @DisplayName("duplicate registration number throws CONFLICT")
        void register_duplicateRegNumber_throwsConflict() {
            when(societyRepository.existsByRegistrationNumber("REG-999")).thenReturn(true);

            assertThatThrownBy(() -> societyService.register(registerRequest, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        }

        @Test
        @DisplayName("duplicate email throws CONFLICT")
        void register_duplicateEmail_throwsConflict() {
            when(societyRepository.existsByRegistrationNumber("REG-999")).thenReturn(false);
            when(userRepository.findByEmail("admin@greenheights.com")).thenReturn(Optional.of(adminUser));

            assertThatThrownBy(() -> societyService.register(registerRequest, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        }
    }

    @Nested
    @DisplayName("verify")
    class VerifyTests {

        @Test
        @DisplayName("throws BAD_REQUEST if society is not PENDING_VERIFICATION")
        void verify_notPending_throwsBadRequest() {
            society.setStatus(SocietyStatus.ACTIVE);
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.of(society));

            VerifySocietyRequest request = new VerifySocietyRequest();
            request.setStatus(SocietyStatus.ACTIVE);

            assertThatThrownBy(() -> societyService.verify(SOCIETY_ID, request, "platform-admin-1"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("rejecting without reason throws BAD_REQUEST")
        void verify_rejected_withoutReason_throwsBadRequest() {
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.of(society));

            VerifySocietyRequest request = new VerifySocietyRequest();
            request.setStatus(SocietyStatus.REJECTED);
            request.setRejectionReason("   ");

            assertThatThrownBy(() -> societyService.verify(SOCIETY_ID, request, "platform-admin-1"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("rejecting with reason updates status to REJECTED")
        void verify_rejected_withReason_success() {
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.of(society));
            when(societyRepository.save(any(Society.class))).thenAnswer(invocation -> invocation.getArgument(0));

            VerifySocietyRequest request = new VerifySocietyRequest();
            request.setStatus(SocietyStatus.REJECTED);
            request.setRejectionReason("Incomplete paperwork");

            SocietyResponse response = societyService.verify(SOCIETY_ID, request, "platform-admin-1");

            assertThat(response.getStatus()).isEqualTo(SocietyStatus.REJECTED);
            assertThat(society.getRejectionReason()).isEqualTo("Incomplete paperwork");
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("approving society sets ACTIVE, trial expiration, and activates super admin")
        void verify_approved_success() {
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.of(society));
            when(societyRepository.save(any(Society.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(adminUser));

            VerifySocietyRequest request = new VerifySocietyRequest();
            request.setStatus(SocietyStatus.ACTIVE);

            SocietyResponse response = societyService.verify(SOCIETY_ID, request, "platform-admin-1");

            assertThat(response.getStatus()).isEqualTo(SocietyStatus.ACTIVE);
            assertThat(society.getVerifiedAt()).isNotNull();
            assertThat(society.getVerifiedBy()).isEqualTo("platform-admin-1");
            assertThat(society.getTrialEndsAt()).isNotNull();
            assertThat(adminUser.getStatus()).isEqualTo(Status.ACTIVE);
            verify(userRepository).save(adminUser);
        }
    }

    @Nested
    @DisplayName("getByJoinCode")
    class GetByJoinCodeTests {

        @Test
        @DisplayName("invalid join code throws NOT_FOUND")
        void getByJoinCode_notFound_throwsNotFound() {
            when(societyRepository.findByJoinCode("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> societyService.getByJoinCode("UNKNOWN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("inactive society throws FORBIDDEN")
        void getByJoinCode_inactive_throwsForbidden() {
            society.setStatus(SocietyStatus.PENDING_VERIFICATION);
            when(societyRepository.findByJoinCode(JOIN_CODE)).thenReturn(Optional.of(society));

            assertThatThrownBy(() -> societyService.getByJoinCode(JOIN_CODE))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        }

        @Test
        @DisplayName("active society returns SocietyResponse")
        void getByJoinCode_active_success() {
            society.setStatus(SocietyStatus.ACTIVE);
            when(societyRepository.findByJoinCode(JOIN_CODE)).thenReturn(Optional.of(society));

            SocietyResponse response = societyService.getByJoinCode(JOIN_CODE);

            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Green Heights");
            assertThat(response.getJoinCode()).isEqualTo(JOIN_CODE);
        }
    }

    @Nested
    @DisplayName("listAll")
    class ListAllTests {

        @Test
        @DisplayName("lists societies with status filter")
        void listAll_withStatus_success() {
            Page<Society> page = new PageImpl<>(List.of(society));
            when(societyRepository.findByStatus(eq(SocietyStatus.PENDING_VERIFICATION), any(Pageable.class)))
                    .thenReturn(page);

            PagedResponse<SocietyResponse> response = societyService.listAll(SocietyStatus.PENDING_VERIFICATION, 0, 10);

            assertThat(response.getContent()).hasSize(1);
            assertThat(response.getContent().get(0).getName()).isEqualTo("Green Heights");
        }

        @Test
        @DisplayName("lists all societies without status filter")
        void listAll_withoutStatus_success() {
            Page<Society> page = new PageImpl<>(List.of(society));
            when(societyRepository.findAll(any(Pageable.class))).thenReturn(page);

            PagedResponse<SocietyResponse> response = societyService.listAll(null, 0, 10);

            assertThat(response.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getMySociety")
    class GetMySocietyTests {

        @Test
        @DisplayName("returns society matching TenantContext")
        void getMySociety_success() {
            TenantContext.setSocietyId(SOCIETY_ID);
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.of(society));

            SocietyResponse response = societyService.getMySociety();

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(SOCIETY_ID);
        }

        @Test
        @DisplayName("throws NOT_FOUND if society not found")
        void getMySociety_notFound_throwsNotFound() {
            TenantContext.setSocietyId("non-existent");
            when(societyRepository.findById("non-existent")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> societyService.getMySociety())
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("getDocumentUrl")
    class GetDocumentUrlTests {

        @Test
        @DisplayName("throws NOT_FOUND if documentUrl is null")
        void getDocumentUrl_nullDoc_throwsNotFound() {
            society.setDocumentUrl(null);
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.of(society));

            assertThatThrownBy(() -> societyService.getDocumentUrl(SOCIETY_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        @DisplayName("generates presigned URL if document exists")
        void getDocumentUrl_success() {
            society.setDocumentUrl("s3/key/doc.pdf");
            when(societyRepository.findById(SOCIETY_ID)).thenReturn(Optional.of(society));
            when(s3Service.generatePresignedUrl("s3/key/doc.pdf")).thenReturn("https://s3.aws.com/presigned-url");

            String url = societyService.getDocumentUrl(SOCIETY_ID);

            assertThat(url).isEqualTo("https://s3.aws.com/presigned-url");
        }
    }
}
