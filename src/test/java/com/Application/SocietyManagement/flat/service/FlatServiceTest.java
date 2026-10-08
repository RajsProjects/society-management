package com.Application.SocietyManagement.flat.service;

import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.flat.dto.CreateFlatRequest;
import com.Application.SocietyManagement.flat.dto.FlatResponse;
import com.Application.SocietyManagement.flat.entity.Flat;
import com.Application.SocietyManagement.flat.enums.FlatType;
import com.Application.SocietyManagement.flat.repository.FlatRepository;
import com.Application.SocietyManagement.users.dto.PagedResponse;
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
@DisplayName("FlatService")
class FlatServiceTest {

    @Mock
    private FlatRepository flatRepository;

    @InjectMocks
    private FlatService flatService;

    private static final String SOCIETY_ID = "society-123";
    private Flat flat;
    private CreateFlatRequest request;

    @BeforeEach
    void setUp() {
        TenantContext.setSocietyId(SOCIETY_ID);

        request = new CreateFlatRequest();
        request.setBlock("A");
        request.setFloor(3);
        request.setFlatNumber("A-301");
        request.setOwnerName("John Doe");
        request.setOwnerEmail("john@example.com");
        request.setOwnerPhone("9876543210");
        request.setOccupied(true);
        request.setType(FlatType.APARTMENT);
        request.setAreaSqFt(1200);

        flat = Flat.builder()
                .societyId(SOCIETY_ID)
                .block("A")
                .floor(3)
                .flatNumber("A-301")
                .ownerName("John Doe")
                .ownerEmail("john@example.com")
                .ownerPhone("9876543210")
                .occupied(true)
                .type(FlatType.APARTMENT)
                .areaSqFt(1200)
                .build();
        flat.setId("flat-001");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("create - successfully creates flat")
    void create_success() {
        when(flatRepository.existsByFlatNumberAndSocietyId("A-301", SOCIETY_ID)).thenReturn(false);
        when(flatRepository.save(any(Flat.class))).thenReturn(flat);

        FlatResponse response = flatService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getFlatNumber()).isEqualTo("A-301");
        assertThat(response.getBlock()).isEqualTo("A");
        assertThat(response.getOwnerName()).isEqualTo("John Doe");
        verify(flatRepository).save(any(Flat.class));
    }

    @Test
    @DisplayName("create - throws 409 when flat number already exists in society")
    void create_conflict() {
        when(flatRepository.existsByFlatNumberAndSocietyId("A-301", SOCIETY_ID)).thenReturn(true);

        assertThatThrownBy(() -> flatService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Flat number already exists");

        verify(flatRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - throws 403 when no tenant context is set")
    void create_missingTenantContext() {
        TenantContext.clear();

        assertThatThrownBy(() -> flatService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("No society context");
    }

    @Test
    @DisplayName("getAll - returns paged flats with block and occupied filters")
    void getAll_withFilters() {
        Page<Flat> page = new PageImpl<>(List.of(flat));
        when(flatRepository.findBySocietyIdAndBlockAndOccupied(eq(SOCIETY_ID), eq("A"), eq(true), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<FlatResponse> result = flatService.getAll("A", true, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFlatNumber()).isEqualTo("A-301");
    }

    @Test
    @DisplayName("getAll - returns all flats when no filter is provided")
    void getAll_noFilters() {
        Page<Flat> page = new PageImpl<>(List.of(flat));
        when(flatRepository.findBySocietyId(eq(SOCIETY_ID), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<FlatResponse> result = flatService.getAll(null, null, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        verify(flatRepository).findBySocietyId(eq(SOCIETY_ID), any(Pageable.class));
    }

    @Test
    @DisplayName("getById - returns flat when found")
    void getById_found() {
        when(flatRepository.findByIdAndSocietyId("flat-001", SOCIETY_ID))
                .thenReturn(Optional.of(flat));

        FlatResponse response = flatService.getById("flat-001");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("flat-001");
    }

    @Test
    @DisplayName("getById - throws 404 when flat not found")
    void getById_notFound() {
        when(flatRepository.findByIdAndSocietyId("flat-999", SOCIETY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> flatService.getById("flat-999"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Flat not found");
    }

    @Test
    @DisplayName("update - successfully updates flat details")
    void update_success() {
        when(flatRepository.findByIdAndSocietyId("flat-001", SOCIETY_ID))
                .thenReturn(Optional.of(flat));
        when(flatRepository.save(any(Flat.class))).thenReturn(flat);

        request.setOwnerName("Jane Doe");
        FlatResponse response = flatService.update("flat-001", request);

        assertThat(response).isNotNull();
        verify(flatRepository).save(flat);
    }

    @Test
    @DisplayName("delete - successfully removes flat")
    void delete_success() {
        when(flatRepository.findByIdAndSocietyId("flat-001", SOCIETY_ID))
                .thenReturn(Optional.of(flat));

        flatService.delete("flat-001");

        verify(flatRepository).delete(flat);
    }

    @Test
    @DisplayName("delete - throws 404 when flat does not exist")
    void delete_notFound() {
        when(flatRepository.findByIdAndSocietyId("flat-999", SOCIETY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> flatService.delete("flat-999"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Flat not found");

        verify(flatRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getById - masks owner PII for other residents")
    void getById_asResident_masksOwnerPii() {
        when(flatRepository.findByIdAndSocietyId("flat-001", SOCIETY_ID))
                .thenReturn(Optional.of(flat));

        com.Application.SocietyManagement.users.entity.User resident =
                com.Application.SocietyManagement.users.entity.User.builder()
                        .role(com.Application.SocietyManagement.users.enums.Roles.RESIDENT)
                        .email("other@example.com")
                        .phone("1111111111")
                        .flatId("flat-other")
                        .build();

        FlatResponse response = flatService.getById("flat-001", resident);

        assertThat(response).isNotNull();
        assertThat(response.getOwnerEmail()).isEqualTo("j***@example.com");
        assertThat(response.getOwnerPhone()).isEqualTo("******3210");
    }

    @Test
    @DisplayName("getById - exposes full PII for ADMIN")
    void getById_asAdmin_exposesOwnerPii() {
        when(flatRepository.findByIdAndSocietyId("flat-001", SOCIETY_ID))
                .thenReturn(Optional.of(flat));

        com.Application.SocietyManagement.users.entity.User admin =
                com.Application.SocietyManagement.users.entity.User.builder()
                        .role(com.Application.SocietyManagement.users.enums.Roles.ADMIN)
                        .email("admin@example.com")
                        .build();

        FlatResponse response = flatService.getById("flat-001", admin);

        assertThat(response).isNotNull();
        assertThat(response.getOwnerEmail()).isEqualTo("john@example.com");
        assertThat(response.getOwnerPhone()).isEqualTo("9876543210");
    }

    @Test
    @DisplayName("getById - exposes full PII for flat owner")
    void getById_asFlatOwner_exposesOwnPii() {
        when(flatRepository.findByIdAndSocietyId("flat-001", SOCIETY_ID))
                .thenReturn(Optional.of(flat));

        com.Application.SocietyManagement.users.entity.User owner =
                com.Application.SocietyManagement.users.entity.User.builder()
                        .role(com.Application.SocietyManagement.users.enums.Roles.RESIDENT)
                        .email("john@example.com")
                        .phone("9876543210")
                        .flatId("flat-001")
                        .build();

        FlatResponse response = flatService.getById("flat-001", owner);

        assertThat(response).isNotNull();
        assertThat(response.getOwnerEmail()).isEqualTo("john@example.com");
        assertThat(response.getOwnerPhone()).isEqualTo("9876543210");
    }
}
