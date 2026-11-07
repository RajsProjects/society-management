package com.Application.SocietyManagement.complaint.controller;

import com.Application.SocietyManagement.complaint.dto.ComplaintResponse;
import com.Application.SocietyManagement.complaint.dto.CreateComplaintRequest;
import com.Application.SocietyManagement.complaint.dto.UpdateComplaintRequest;
import com.Application.SocietyManagement.complaint.enums.ComplaintCategory;
import com.Application.SocietyManagement.complaint.enums.ComplaintStatus;
import com.Application.SocietyManagement.complaint.service.ComplaintService;
import com.Application.SocietyManagement.core.exception.GlobalExceptionHandler;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComplaintController Unit & Contract Test Suite")
class ComplaintControllerTest {

    @Mock
    private ComplaintService complaintService;

    @InjectMocks
    private ComplaintController complaintController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId("user-101");
        testUser.setEmail("resident@example.com");
        testUser.setRole(Roles.RESIDENT);
        testUser.setSocietyId("society-101");

        // Custom argument resolver to inject testUser into @AuthenticationPrincipal parameters
        HandlerMethodArgumentResolver authPrincipalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return testUser;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(complaintController)
                .setCustomArgumentResolvers(authPrincipalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/complaints - Successfully creates a complaint")
    void create_validRequest_returns201Created() throws Exception {
        CreateComplaintRequest request = new CreateComplaintRequest();
        request.setTitle("Elevator Malfunction");
        request.setDescription("Elevator in Tower B is stuck between floors.");
        request.setCategory(ComplaintCategory.MAINTENANCE);

        ComplaintResponse response = ComplaintResponse.builder()
                .id("comp-1")
                .societyId("society-101")
                .residentId("user-101")
                .title("Elevator Malfunction")
                .status(ComplaintStatus.OPEN)
                .build();

        when(complaintService.create(any(CreateComplaintRequest.class), any(User.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("comp-1"))
                .andExpect(jsonPath("$.title").value("Elevator Malfunction"))
                .andExpect(jsonPath("$.status").value("OPEN"));

        verify(complaintService).create(any(CreateComplaintRequest.class), eq(testUser));
    }

    @Test
    @DisplayName("POST /api/v1/complaints - Validation failure returns 400 Bad Request")
    void create_invalidRequest_returns400BadRequest() throws Exception {
        CreateComplaintRequest request = new CreateComplaintRequest();
        // Missing mandatory title, description, and category

        mockMvc.perform(post("/api/v1/complaints")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"));

        verify(complaintService, never()).create(any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/complaints - Returns paginated list of complaints")
    void getAll_returnsPagedResponse() throws Exception {
        PagedResponse<ComplaintResponse> pagedResponse = PagedResponse.<ComplaintResponse>builder()
                .content(Collections.emptyList())
                .page(0)
                .size(20)
                .totalElements(0)
                .totalPages(0)
                .build();

        when(complaintService.getComplaints(isNull(), isNull(), eq(0), eq(20), any(User.class)))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/complaints"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));

        verify(complaintService).getComplaints(isNull(), isNull(), eq(0), eq(20), eq(testUser));
    }

    @Test
    @DisplayName("GET /api/v1/complaints/{id} - Returns single complaint details")
    void getById_returnsComplaint() throws Exception {
        ComplaintResponse response = ComplaintResponse.builder()
                .id("comp-123")
                .title("Water Leak")
                .status(ComplaintStatus.OPEN)
                .build();

        when(complaintService.getById(eq("comp-123"), any(User.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/complaints/comp-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("comp-123"))
                .andExpect(jsonPath("$.title").value("Water Leak"));

        verify(complaintService).getById("comp-123", testUser);
    }

    @Test
    @DisplayName("PATCH /api/v1/complaints/{id}/status - Updates status and note")
    void updateStatus_returnsUpdatedComplaint() throws Exception {
        UpdateComplaintRequest request = new UpdateComplaintRequest();
        request.setStatus(ComplaintStatus.RESOLVED);
        request.setAdminNote("Fixed by plumber");

        ComplaintResponse response = ComplaintResponse.builder()
                .id("comp-123")
                .status(ComplaintStatus.RESOLVED)
                .adminNote("Fixed by plumber")
                .build();

        when(complaintService.updateStatus(eq("comp-123"), any(UpdateComplaintRequest.class), any(User.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/complaints/comp-123/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("comp-123"))
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.adminNote").value("Fixed by plumber"));

        verify(complaintService).updateStatus(eq("comp-123"), any(UpdateComplaintRequest.class), eq(testUser));
    }

    @Test
    @DisplayName("DELETE /api/v1/complaints/{id} - Returns 204 No Content")
    void delete_returns204NoContent() throws Exception {
        doNothing().when(complaintService).delete(eq("comp-123"), any(User.class));

        mockMvc.perform(delete("/api/v1/complaints/comp-123"))
                .andExpect(status().isNoContent());

        verify(complaintService).delete("comp-123", testUser);
    }

    @Test
    @DisplayName("GET /api/v1/complaints/stats - Returns complaints stats map")
    void getStats_returnsStatsMap() throws Exception {
        Map<String, Long> stats = Map.of(
                "total", 10L,
                "open", 3L,
                "inProgress", 2L,
                "resolved", 5L
        );

        when(complaintService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/complaints/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10))
                .andExpect(jsonPath("$.open").value(3))
                .andExpect(jsonPath("$.inProgress").value(2))
                .andExpect(jsonPath("$.resolved").value(5));

        verify(complaintService).getStats();
    }
}

