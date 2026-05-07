package com.Application.SocietyManagement.users.communication.service;

import com.Application.SocietyManagement.communication.dto.AnnouncementRequest;
import com.Application.SocietyManagement.communication.dto.AnnouncementResponse;
import com.Application.SocietyManagement.communication.entity.Announcement;
import com.Application.SocietyManagement.communication.enums.AnnouncementType;
import com.Application.SocietyManagement.communication.repository.AnnouncementRepository;
import com.Application.SocietyManagement.communication.service.AnnouncementService;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementRepository announcementRepository;

    @InjectMocks
    private AnnouncementService announcementService;

    private Announcement announcement;
    private AnnouncementRequest request;

    @BeforeEach
    void setUp() {
        announcement = Announcement.builder()
                .title("Water Supply Interruption")
                .content("Water will be cut from 2PM to 4PM")
                .type(AnnouncementType.MAINTENANCE)
                .authorId("admin123")
                .build();

        request = new AnnouncementRequest();
        request.setTitle("Water Supply Interruption");
        request.setContent("Water will be cut from 2PM to 4PM");
        request.setType(AnnouncementType.MAINTENANCE);
    }

    // ── create tests ──

    @Test
    void create_success_returnsResponse() {
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AnnouncementResponse response =
                announcementService.create(request, "admin123");

        assertThat(response.getTitle()).isEqualTo("Water Supply Interruption");
        assertThat(response.getContent()).isEqualTo("Water will be cut from 2PM to 4PM");
        assertThat(response.getType()).isEqualTo(AnnouncementType.MAINTENANCE);
        assertThat(response.getAuthorId()).isEqualTo("admin123");
    }

    @Test
    void create_authorIdIsSetFromParameter() {
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        announcementService.create(request, "admin456");

        verify(announcementRepository).save(argThat(a ->
                a.getAuthorId().equals("admin456")
        ));
    }

    @Test
    void create_savedWithCorrectType() {
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AnnouncementResponse response =
                announcementService.create(request, "admin123");

        assertThat(response.getType()).isEqualTo(AnnouncementType.MAINTENANCE);
    }

    @Test
    void create_emergencyType_savedCorrectly() {
        request.setType(AnnouncementType.EMERGENCY);
        when(announcementRepository.save(any(Announcement.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AnnouncementResponse response =
                announcementService.create(request, "admin123");

        assertThat(response.getType()).isEqualTo(AnnouncementType.EMERGENCY);
    }

    // ── getAll tests ──

    @Test
    void getAll_noFilter_returnsAllAnnouncements() {
        Page<Announcement> page = new PageImpl<>(List.of(announcement));
        when(announcementRepository.findAll(any(Pageable.class))).thenReturn(page);

        PagedResponse<AnnouncementResponse> result =
                announcementService.getAll(null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(announcementRepository).findAll(any(Pageable.class));
        verify(announcementRepository, never()).findByType(any(), any());
    }

    @Test
    void getAll_withTypeFilter_callsFilteredRepository() {
        Page<Announcement> page = new PageImpl<>(List.of(announcement));
        when(announcementRepository.findByType(
                eq(AnnouncementType.MAINTENANCE), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<AnnouncementResponse> result =
                announcementService.getAll(AnnouncementType.MAINTENANCE, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getType())
                .isEqualTo(AnnouncementType.MAINTENANCE);
        verify(announcementRepository).findByType(
                eq(AnnouncementType.MAINTENANCE), any(Pageable.class));
        verify(announcementRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getAll_emptyResult_returnsEmptyPage() {
        Page<Announcement> emptyPage = new PageImpl<>(List.of());
        when(announcementRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        PagedResponse<AnnouncementResponse> result =
                announcementService.getAll(null, 0, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getAll_paginationMetadata_isCorrect() {
        Page<Announcement> page = new PageImpl<>(
                List.of(announcement),
                org.springframework.data.domain.PageRequest.of(0, 20),
                45
        );
        when(announcementRepository.findAll(any(Pageable.class))).thenReturn(page);

        PagedResponse<AnnouncementResponse> result =
                announcementService.getAll(null, 0, 20);

        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(45);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }

    // ── getById tests ──

    @Test
    void getById_found_returnsResponse() {
        when(announcementRepository.findById("ann1"))
                .thenReturn(Optional.of(announcement));

        AnnouncementResponse response = announcementService.getById("ann1");

        assertThat(response.getTitle()).isEqualTo("Water Supply Interruption");
        assertThat(response.getType()).isEqualTo(AnnouncementType.MAINTENANCE);
    }

    @Test
    void getById_notFound_throwsNotFound() {
        when(announcementRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> announcementService.getById("nonexistent"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Announcement not found");
    }

    // ── update tests ──

    @Test
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
    void update_notFound_throwsNotFound() {
        when(announcementRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> announcementService.update("nonexistent", request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Announcement not found");

        verify(announcementRepository, never()).save(any());
    }

    @Test
    void update_persistsAllFields() {
        AnnouncementRequest updateRequest = new AnnouncementRequest();
        updateRequest.setTitle("New Title");
        updateRequest.setContent("New Content");
        updateRequest.setType(AnnouncementType.EMERGENCY);

        when(announcementRepository.findById("ann1"))
                .thenReturn(Optional.of(announcement));
        when(announcementRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        announcementService.update("ann1", updateRequest);

        verify(announcementRepository).save(argThat(a ->
                a.getTitle().equals("New Title") &&
                        a.getContent().equals("New Content") &&
                        a.getType() == AnnouncementType.EMERGENCY
        ));
    }

    // ── delete tests ──

    @Test
    void delete_success_deletesAnnouncement() {
        when(announcementRepository.findById("ann1"))
                .thenReturn(Optional.of(announcement));

        announcementService.delete("ann1");

        verify(announcementRepository).delete(announcement);
    }

    @Test
    void delete_notFound_throwsNotFound() {
        when(announcementRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> announcementService.delete("nonexistent"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Announcement not found");

        verify(announcementRepository, never()).delete(any());
    }
}
