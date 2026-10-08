package com.Application.SocietyManagement.society.controller;

import com.Application.SocietyManagement.society.dto.SocietyResponse;
import com.Application.SocietyManagement.society.dto.VerifySocietyRequest;
import com.Application.SocietyManagement.society.enums.SocietyStatus;
import com.Application.SocietyManagement.society.service.SocietyService;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.entity.User;
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
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocietyController")
class SocietyControllerTest {

    @Mock
    private SocietyService societyService;

    @InjectMocks
    private SocietyController societyController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User platformAdminUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        platformAdminUser = User.builder().email("platformadmin@example.com").build();
        platformAdminUser.setId("admin-user-id");

        HandlerMethodArgumentResolver userResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(@NonNull MethodParameter parameter) {
                return parameter.getParameterType().equals(User.class);
            }

            @Override
            public Object resolveArgument(@NonNull MethodParameter parameter,
                                          @Nullable ModelAndViewContainer mavContainer,
                                          @NonNull NativeWebRequest webRequest,
                                          @Nullable WebDataBinderFactory binderFactory) {
                return platformAdminUser;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(societyController)
                .setCustomArgumentResolvers(userResolver)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/societies/join/{joinCode} returns society details")
    void getByJoinCode_success() throws Exception {
        SocietyResponse response = SocietyResponse.builder()
                .id("soc-1")
                .name("Palm Meadows")
                .joinCode("JOIN123")
                .status(SocietyStatus.ACTIVE)
                .build();

        when(societyService.getByJoinCode("JOIN123")).thenReturn(response);

        mockMvc.perform(get("/api/v1/societies/join/JOIN123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("soc-1"))
                .andExpect(jsonPath("$.name").value("Palm Meadows"))
                .andExpect(jsonPath("$.joinCode").value("JOIN123"));
    }

    @Test
    @DisplayName("GET /api/v1/societies/me returns authenticated society")
    void getMySociety_success() throws Exception {
        SocietyResponse response = SocietyResponse.builder()
                .id("soc-1")
                .name("Palm Meadows")
                .status(SocietyStatus.ACTIVE)
                .build();

        when(societyService.getMySociety()).thenReturn(response);

        mockMvc.perform(get("/api/v1/societies/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Palm Meadows"));
    }

    @Test
    @DisplayName("GET /api/v1/societies lists paginated societies")
    void listAll_success() throws Exception {
        SocietyResponse response = SocietyResponse.builder()
                .id("soc-1")
                .name("Palm Meadows")
                .build();

        PagedResponse<SocietyResponse> pagedResponse = PagedResponse.<SocietyResponse>builder()
                .content(List.of(response))
                .page(0)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .build();

        when(societyService.listAll(eq(SocietyStatus.ACTIVE), eq(0), eq(10))).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/societies")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Palm Meadows"));
    }

    @Test
    @DisplayName("GET /api/v1/societies/{id}/document returns presigned URL")
    void getDocumentUrl_success() throws Exception {
        when(societyService.getDocumentUrl("soc-1")).thenReturn("https://s3.aws.com/doc.pdf");

        mockMvc.perform(get("/api/v1/societies/soc-1/document"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://s3.aws.com/doc.pdf"));
    }

    @Test
    @DisplayName("PATCH /api/v1/societies/{id}/verify approves society")
    void verify_success() throws Exception {
        VerifySocietyRequest request = new VerifySocietyRequest();
        request.setStatus(SocietyStatus.ACTIVE);

        SocietyResponse response = SocietyResponse.builder()
                .id("soc-1")
                .status(SocietyStatus.ACTIVE)
                .build();

        when(societyService.verify(eq("soc-1"), any(VerifySocietyRequest.class), eq("admin-user-id")))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/societies/soc-1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
