package com.Application.SocietyManagement.communication.service;

import com.Application.SocietyManagement.communication.dto.AnnouncementRequest;
import com.Application.SocietyManagement.communication.dto.AnnouncementResponse;
import com.Application.SocietyManagement.communication.entity.Announcement;
import com.Application.SocietyManagement.communication.enums.AnnouncementType;
import com.Application.SocietyManagement.communication.repository.AnnouncementRepository;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    public AnnouncementResponse create(AnnouncementRequest request, String authorId) {
        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .type(request.getType())
                .authorId(authorId)
                .build();

        return AnnouncementResponse.from(announcementRepository.save(announcement));
    }

    public PagedResponse<AnnouncementResponse> getAll(AnnouncementType type,
                                                      int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Announcement> result = type != null
                ? announcementRepository.findByType(type, pageable)
                : announcementRepository.findAll(pageable);

        return PagedResponse.<AnnouncementResponse>builder()
                .content(result.getContent().stream()
                        .map(AnnouncementResponse::from)
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    public AnnouncementResponse getById(String id) {
        return AnnouncementResponse.from(findById(id));
    }

    public AnnouncementResponse update(String id, AnnouncementRequest request) {
        Announcement announcement = findById(id);
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setType(request.getType());
        return AnnouncementResponse.from(announcementRepository.save(announcement));
    }

    public void delete(String id) {
        Announcement announcement = findById(id);
        announcementRepository.delete(announcement);
    }

    private Announcement findById(String id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Announcement not found"));
    }
}
