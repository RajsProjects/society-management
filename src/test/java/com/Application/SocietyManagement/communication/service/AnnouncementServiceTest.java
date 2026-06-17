package com.Application.SocietyManagement.communication.service;

import com.Application.SocietyManagement.communication.dto.AnnouncementRequest;
import com.Application.SocietyManagement.communication.dto.AnnouncementResponse;
import com.Application.SocietyManagement.communication.entity.Announcement;
import com.Application.SocietyManagement.communication.enums.AnnouncementType;
import com.Application.SocietyManagement.communication.repository.AnnouncementRepository;
import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnnouncementService")
class AnnouncementServiceTest {

    @Mock private AnnouncementRepository announcementRepository;

    @InjectMocks
    private AnnouncementService announcementService;

    private Announcement announcement;
    private AnnouncementRequest request;

    @BeforeEach
    void setUp() {
        TenantContext.setSocietyId("test-society-id");

        announcement = Announcement.builder()
                .title("Water Supply Interruption")
                .content("Water will be cut from 2PM to 4PM")
                .type(AnnouncementType.MAINTENANCE)
                .authorId("admin123")
                .societyId("test-society-id")
                .build();

        request = new AnnouncementRequest();
        request.setTitle("Water Supply Interruption");
        request.setContent("Water will be cut from 2PM to 4PM");
        request.setType(AnnouncementType.MAINTENANCE);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("success - returns created announcement")
        void create_success_returnsResponse() {
            when(announcementRepository.save(any(Announcement.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            AnnouncementResponse response =
                    announcementService.create(request, "admin123");

            assertThat(response.getTitle())
                    .isEqualTo("Water Supply Interruption");
            assertThat(response.getContent())
                    .isEqualTo("Water will be cut from 2PM to 4PM");
            assertThat(response.getType())
                    .isEqualTo(AnnouncementType.MAINTENANCE);
            assertThat(response.getAuthorId()).isEqualTo("admin123");
        }

        @Test
        @DisplayName("authorId set from parameter")
        void create_authorIdIsSetFromParameter() {
            when(announcementRepository.save(any(Announcement.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            announcementService.create(request, "admin456");

            verify(announcementRepository).save(argThat(a ->
                    a.getAuthorId().equals("admin456")));
        }

        @Test
        @DisplayName("societyId set from TenantContext")
        void create_societyIdSetFromTenantContext() {
            when(announcementRepository.save(any(Announcement.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            announcementService.create(request, "admin123");

            verify(announcementRepository).save(argThat(a ->
                    "test-society-id".equals(a.getSocietyId())));
        }

        @Test
        @DisplayName("EMERGENCY type - saved correctly")
        void create_emergencyType_savedCorrectly() {
            request.setType(AnnouncementType.EMERGENCY);
            when(announcementRepository.save(any(Announcement.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            AnnouncementResponse response =
                    announcementService.create(request, "admin123");

            assertThat(response.getType()).isEqualTo(AnnouncementType.EMERGENCY);
        }

        @Test
        @DisplayName("GENERAL type - saved correctly")
        void create_generalType_savedCorrectly() {
            request.setType(AnnouncementType.GENERAL);
            when(announcementRepository.save(any(Announcement.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            AnnouncementResponse response =
                    announcementService.create(request, "admin123");

            assertThat(response.getType()).isEqualTo(AnnouncementType.GENERAL);
        }
    }

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("no filter - returns all for society")
        void getAll_noFilter_returnsAllAnnouncements() {
            Page<Announcement> page = new PageImpl<>(List.of(announcement));
            when(announcementRepository.findBySocietyId(
                    eq("test-society-id"), any(Pageable.class)))
                    .thenReturn(page);

            PagedResponse<AnnouncementResponse> result =
                    announcementService.getAll(null, 0, 20);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(announcementRepository).findBySocietyId(
                    eq("test-society-id"), any(Pageable.class));
            verify(announcementRepository, never())
                    .findByTypeAndSocietyId(any(), any(), any());
        }

        @Test
        @DisplayName("filter by MAINTENANCE type - calls filtered repository")
        void getAll_withTypeFilter_callsFilteredRepository() {
            Page<Announcement> page = new PageImpl<>(List.of(announcement));
            when(announcementRepository.findByTypeAndSocietyId(
                    eq(AnnouncementType.MAINTENANCE),
                    eq("test-society-id"),
                    any(Pageable.class)))
                    .thenReturn(page);

            PagedResponse<AnnouncementResponse> result =
                    announcementService.getAll(AnnouncementType.MAINTENANCE, 0, 20);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getType())
                    .isEqualTo(AnnouncementType.MAINTENANCE);
            verify(announcementRepository).findByTypeAndSocietyId(
                    eq(AnnouncementType.MAINTENANCE),
                    eq("test-society-id"),
                    any(Pageable.class));
            verify(announcementRepository, never())
                    .findBySocietyId(any(), any());
        }

        @Test
        @DisplayName("filter by EMERGENCY type")
        void getAll_emergencyFilter_callsFilteredRepository() {
            Announcement emergency = Announcement.builder()
                    .title("Emergency!")
                    .content("Urgent alert")
                    .type(AnnouncementType.EMERGENCY)
                    .societyId("test-society-id")
                    .build();

            Page<Announcement> page = new PageImpl<>(List.of(emergency));
            when(announcementRepository.findByTypeAndSocietyId(
                    eq(AnnouncementType.EMERGENCY),
                    eq("test-society-id"),
                    any(Pageable.class)))
                    .thenReturn(page);

            PagedResponse<AnnouncementResponse> result =
                    announcementService.getAll(AnnouncementType.EMERGENCY, 0, 20);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getType())
                    .isEqualTo(AnnouncementType.EMERGENCY);
        }

        @Test
        @DisplayName("empty result - returns empty page")
        void getAll_emptyResult_returnsEmptyPage() {
            Page<Announcement> emptyPage = new PageImpl<>(List.of());
            when(announcementRepository.findBySocietyId(
                    eq("test-society-id"), any(Pageable.class)))
                    .thenReturn(emptyPage);

            PagedResponse<AnnouncementResponse> result =
                    announcementService.getAll(null, 0, 20);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("pagination metadata - correct")
        void getAll_paginationMetadata_isCorrect() {
            Page<Announcement> page = new PageImpl<>(
                    List.of(announcement),
                    PageRequest.of(0, 20),
                    45
            );
            when(announcementRepository.findBySocietyId(
                    eq("test-society-id"), any(Pageable.class)))
                    .thenReturn(page);

            PagedResponse<AnnouncementResponse> result =
                    announcementService.getAll(null, 0, 20);

            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(20);
            assertThat(result.getTotalElements()).isEqualTo(45);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("found - returns correct response")
        void getById_found_returnsResponse() {
            when(announcementRepository.findById("ann1"))
                    .thenReturn(Optional.of(announcement));

            AnnouncementResponse response =
                    announcementService.getById("ann1");

            assertThat(response.getTitle())
                    .isEqualTo("Water Supply Interruption");
            assertThat(response.getType())
                    .isEqualTo(AnnouncementType.MAINTENANCE);
        }

        @Test
        @DisplayName("not found - throws 404")
        void getById_notFound_throwsNotFound() {
            when(announcementRepository.findById("nonexistent"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    announcementService.getById("nonexistent"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(404);
                        assertThat(e.getMessage()).contains("Announcement not found");
                    });
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("success - returns updated response")
        void update_success_returnsUpdatedResponse() {
            AnnouncementRequest updateRequest = new AnnouncementRequest();
            updateRequest.setTitle("Updated Title");
            updateRequest.setContent("Updated content");
            updateRequest.setType(AnnouncementType.GENERAL);

            when(announcementRepository.findById("ann1"))
                    .thenReturn(Optional.of(announcement));
            when(announcementRepository.save(any(Announcement.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            AnnouncementResponse response =
                    announcementService.update("ann1", updateRequest);

            assertThat(response.getTitle()).isEqualTo("Updated Title");
            assertThat(response.getContent()).isEqualTo("Updated content");
            assertThat(response.getType()).isEqualTo(AnnouncementType.GENERAL);
        }

        @Test
        @DisplayName("not found - throws 404")
        void update_notFound_throwsNotFound() {
            when(announcementRepository.findById("nonexistent"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    announcementService.update("nonexistent", request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(404);
                    });

            verify(announcementRepository, never()).save(any());
        }

        @Test
        @DisplayName("persists all updated fields")
        void update_persistsAllFields() {
            AnnouncementRequest updateRequest = new AnnouncementRequest();
            updateRequest.setTitle("New Title");
            updateRequest.setContent("New Content");
            updateRequest.setType(AnnouncementType.EMERGENCY);

            when(announcementRepository.findById("ann1"))
                    .thenReturn(Optional.of(announcement));
            when(announcementRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            announcementService.update("ann1", updateRequest);

            verify(announcementRepository).save(argThat(a ->
                    a.getTitle().equals("New Title") &&
                            a.getContent().equals("New Content") &&
                            a.getType() == AnnouncementType.EMERGENCY
            ));
        }
    }

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("success - deletes announcement")
        void delete_success_deletesAnnouncement() {
            when(announcementRepository.findById("ann1"))
                    .thenReturn(Optional.of(announcement));

            announcementService.delete("ann1");

            verify(announcementRepository).delete(announcement);
        }

        @Test
        @DisplayName("not found - throws 404")
        void delete_notFound_throwsNotFound() {
            when(announcementRepository.findById("nonexistent"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    announcementService.delete("nonexistent"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(404);
                    });

            verify(announcementRepository, never()).delete(any());
        }
    }
}