package com.Application.SocietyManagement.complaint.service;

import com.Application.SocietyManagement.complaint.dto.ComplaintResponse;
import com.Application.SocietyManagement.complaint.dto.CreateComplaintRequest;
import com.Application.SocietyManagement.complaint.dto.UpdateComplaintRequest;
import com.Application.SocietyManagement.complaint.entity.Complaint;
import com.Application.SocietyManagement.complaint.enums.ComplaintCategory;
import com.Application.SocietyManagement.complaint.enums.ComplaintStatus;
import com.Application.SocietyManagement.complaint.repository.ComplaintRepository;
import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComplaintService")
class ComplaintServiceTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @InjectMocks
    private ComplaintService complaintService;

    private static final String SOCIETY_ID = "society-123";
    private User residentUser;
    private User adminUser;
    private User otherResidentUser;
    private Complaint complaint;
    private CreateComplaintRequest createRequest;

    @BeforeEach
    void setUp() {
        TenantContext.setSocietyId(SOCIETY_ID);

        residentUser = User.builder()
                .email("resident@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(Roles.RESIDENT)
                .status(Status.ACTIVE)
                .societyId(SOCIETY_ID)
                .flatId("A-101")
                .build();
        residentUser.setId("resident-1");

        adminUser = User.builder()
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .role(Roles.ADMIN)
                .status(Status.ACTIVE)
                .societyId(SOCIETY_ID)
                .build();
        adminUser.setId("admin-1");

        otherResidentUser = User.builder()
                .email("other@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .role(Roles.RESIDENT)
                .status(Status.ACTIVE)
                .societyId(SOCIETY_ID)
                .flatId("B-202")
                .build();
        otherResidentUser.setId("resident-2");

        createRequest = new CreateComplaintRequest();
        createRequest.setTitle("Leaking Pipe");
        createRequest.setDescription("Pipe leaking in the hallway");
        createRequest.setCategory(ComplaintCategory.PLUMBING);

        complaint = Complaint.builder()
                .societyId(SOCIETY_ID)
                .residentId("resident-1")
                .residentName("John Doe")
                .apartmentNumber("A-101")
                .title("Leaking Pipe")
                .description("Pipe leaking in the hallway")
                .category(ComplaintCategory.PLUMBING)
                .status(ComplaintStatus.OPEN)
                .build();
        complaint.setId("complaint-1");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("create - successfully creates complaint for resident")
    void create_success() {
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        ComplaintResponse response = complaintService.create(createRequest, residentUser);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Leaking Pipe");
        assertThat(response.getResidentName()).isEqualTo("John Doe");
        verify(complaintRepository).save(any(Complaint.class));
    }

    @Test
    @DisplayName("create - throws 403 when no tenant context is set")
    void create_missingTenantContext() {
        TenantContext.clear();

        assertThatThrownBy(() -> complaintService.create(createRequest, residentUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No society context");
    }

    @Test
    @DisplayName("getComplaints - admin sees all complaints")
    void getComplaints_asAdmin() {
        Page<Complaint> page = new PageImpl<>(List.of(complaint));
        when(complaintRepository.findBySocietyId(eq(SOCIETY_ID), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<ComplaintResponse> result = complaintService.getComplaints(
                null, null, 0, 10, adminUser);

        assertThat(result.getContent()).hasSize(1);
        verify(complaintRepository).findBySocietyId(eq(SOCIETY_ID), any(Pageable.class));
    }

    @Test
    @DisplayName("getComplaints - resident sees only their own complaints")
    void getComplaints_asResident() {
        Page<Complaint> page = new PageImpl<>(List.of(complaint));
        when(complaintRepository.findBySocietyIdAndResidentId(eq(SOCIETY_ID), eq("resident-1"), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<ComplaintResponse> result = complaintService.getComplaints(
                null, null, 0, 10, residentUser);

        assertThat(result.getContent()).hasSize(1);
        verify(complaintRepository).findBySocietyIdAndResidentId(eq(SOCIETY_ID), eq("resident-1"), any(Pageable.class));
    }

    @Test
    @DisplayName("getById - resident can view their own complaint")
    void getById_ownerResident_success() {
        when(complaintRepository.findByIdAndSocietyId("complaint-1", SOCIETY_ID))
                .thenReturn(Optional.of(complaint));

        ComplaintResponse response = complaintService.getById("complaint-1", residentUser);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("complaint-1");
    }

    @Test
    @DisplayName("getById - resident cannot view another resident's complaint (403)")
    void getById_otherResident_forbidden() {
        when(complaintRepository.findByIdAndSocietyId("complaint-1", SOCIETY_ID))
                .thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.getById("complaint-1", otherResidentUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    @DisplayName("getById - throws 404 when complaint not found")
    void getById_notFound() {
        when(complaintRepository.findByIdAndSocietyId("complaint-999", SOCIETY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> complaintService.getById("complaint-999", residentUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Complaint not found");
    }

    @Test
    @DisplayName("updateStatus - admin can update complaint status to IN_PROGRESS")
    void updateStatus_asAdmin_success() {
        when(complaintRepository.findByIdAndSocietyId("complaint-1", SOCIETY_ID))
                .thenReturn(Optional.of(complaint));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(complaint);

        UpdateComplaintRequest updateReq = new UpdateComplaintRequest();
        updateReq.setStatus(ComplaintStatus.IN_PROGRESS);
        updateReq.setAdminNote("Plumber assigned");

        ComplaintResponse response = complaintService.updateStatus("complaint-1", updateReq, adminUser);

        assertThat(response).isNotNull();
        verify(complaintRepository).save(complaint);
    }

    @Test
    @DisplayName("delete - resident can delete OPEN complaint")
    void delete_openComplaint_success() {
        when(complaintRepository.findByIdAndSocietyId("complaint-1", SOCIETY_ID))
                .thenReturn(Optional.of(complaint));

        complaintService.delete("complaint-1", residentUser);

        verify(complaintRepository).delete(complaint);
    }

    @Test
    @DisplayName("delete - throws 400 when attempting to delete non-OPEN complaint")
    void delete_nonOpenComplaint_badRequest() {
        complaint.setStatus(ComplaintStatus.RESOLVED);
        when(complaintRepository.findByIdAndSocietyId("complaint-1", SOCIETY_ID))
                .thenReturn(Optional.of(complaint));

        assertThatThrownBy(() -> complaintService.delete("complaint-1", residentUser))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only OPEN complaints can be deleted");

        verify(complaintRepository, never()).delete(any());
    }
}
