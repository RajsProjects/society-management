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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;

    private static final long STATS_TTL_MS = 10_000L;
    private final java.util.concurrent.ConcurrentHashMap<String, CachedComplaintStats> complaintStatsCache =
            new java.util.concurrent.ConcurrentHashMap<>();

    private record CachedComplaintStats(java.util.Map<String, Long> stats, long expiresAt) {}

    private String requireSocietyId() {
        String societyId = TenantContext.getSocietyId();
        if (societyId == null || societyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No society context");
        }
        return societyId;
    }

    private void requireAuthenticated(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
    }

    public ComplaintResponse create(CreateComplaintRequest request,
                                    User currentUser) {
        requireAuthenticated(currentUser);
        String societyId = requireSocietyId();
        String firstName = currentUser.getFirstName() != null ? currentUser.getFirstName().trim() : "";
        String lastName = currentUser.getLastName() != null ? currentUser.getLastName().trim() : "";
        String residentName = (firstName + " " + lastName).trim();

        Complaint complaint = Complaint.builder()
                .societyId(societyId)
                .residentId(currentUser.getId())
                .residentName(residentName.isEmpty() ? currentUser.getEmail() : residentName)
                .apartmentNumber(currentUser.getFlatId())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .status(ComplaintStatus.OPEN)
                .build();

        Complaint saved = complaintRepository.save(complaint);
        complaintStatsCache.remove(societyId);
        log.info("Complaint created by {} for society {}",
                currentUser.getEmail(), saved.getSocietyId());

        return ComplaintResponse.from(saved);
    }

    public PagedResponse<ComplaintResponse> getComplaints(
            ComplaintStatus status,
            ComplaintCategory category,
            int page, int size,
            User currentUser) {

        requireAuthenticated(currentUser);
        String societyId = requireSocietyId();
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Complaint> result;
        boolean isAdmin = currentUser.getRole() == Roles.ADMIN
                || currentUser.getRole() == Roles.SUPER_ADMIN;

        if (isAdmin) {
            // admin sees all complaints for society
            if (status != null && category != null) {
                result = complaintRepository
                        .findBySocietyIdAndStatusAndCategory(
                                societyId, status, category, pageable);
            } else if (status != null) {
                result = complaintRepository
                        .findBySocietyIdAndStatus(
                                societyId, status, pageable);
            } else if (category != null) {
                result = complaintRepository
                        .findBySocietyIdAndCategory(
                                societyId, category, pageable);
            } else {
                result = complaintRepository
                        .findBySocietyId(societyId, pageable);
            }
        } else {
            // resident sees only own complaints
            if (status != null && category != null) {
                result = complaintRepository
                        .findBySocietyIdAndResidentIdAndStatusAndCategory(
                                societyId, currentUser.getId(),
                                status, category, pageable);
            } else if (status != null) {
                result = complaintRepository
                        .findBySocietyIdAndResidentIdAndStatus(
                                societyId, currentUser.getId(),
                                status, pageable);
            } else if (category != null) {
                result = complaintRepository
                        .findBySocietyIdAndResidentIdAndCategory(
                                societyId, currentUser.getId(),
                                category, pageable);
            } else {
                result = complaintRepository
                        .findBySocietyIdAndResidentId(
                                societyId, currentUser.getId(), pageable);
            }
        }

        return PagedResponse.<ComplaintResponse>builder()
                .content(result.getContent().stream()
                        .map(ComplaintResponse::from)
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    public ComplaintResponse getById(String complaintId, User currentUser) {
        requireAuthenticated(currentUser);
        Complaint complaint = findComplaint(complaintId);
        validateAccess(complaint, currentUser);
        return ComplaintResponse.from(complaint);
    }

    public ComplaintResponse updateStatus(String complaintId,
                                          UpdateComplaintRequest request,
                                          User currentUser) {
        requireAuthenticated(currentUser);
        boolean isAdmin = currentUser.getRole() == Roles.ADMIN
                || currentUser.getRole() == Roles.SUPER_ADMIN;
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only administrators can update complaint status");
        }

        Complaint complaint = findComplaint(complaintId);
        validateAccess(complaint, currentUser);

        ComplaintStatus oldStatus = complaint.getStatus();
        complaint.setStatus(request.getStatus());

        if (request.getAdminNote() != null) {
            complaint.setAdminNote(request.getAdminNote());
        }

        if (request.getStatus() == ComplaintStatus.RESOLVED) {
            String first = currentUser.getFirstName() != null ? currentUser.getFirstName().trim() : "";
            String last = currentUser.getLastName() != null ? currentUser.getLastName().trim() : "";
            String resolverName = (first + " " + last).trim();
            complaint.setResolvedBy(!resolverName.isEmpty() ? resolverName : currentUser.getEmail());
        } else {
            complaint.setResolvedBy(null);
        }

        Complaint saved = complaintRepository.save(complaint);
        complaintStatsCache.remove(complaint.getSocietyId());
        log.info("Complaint {} status changed from {} to {} by {}",
                complaintId, oldStatus, request.getStatus(),
                currentUser.getEmail());

        return ComplaintResponse.from(saved);
    }

    public void delete(String complaintId, User currentUser) {
        requireAuthenticated(currentUser);
        Complaint complaint = findComplaint(complaintId);
        validateAccess(complaint, currentUser);

        if (complaint.getStatus() != ComplaintStatus.OPEN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only OPEN complaints can be deleted");
        }

        complaintRepository.delete(complaint);
        complaintStatsCache.remove(complaint.getSocietyId());
        log.info("Complaint {} deleted by {}", complaintId,
                currentUser.getEmail());
    }

    // ── Dashboard stats ──

    public java.util.Map<String, Long> getStats() {
        final String societyId = requireSocietyId();

        long now = System.currentTimeMillis();
        CachedComplaintStats cached = complaintStatsCache.get(societyId);
        if (cached != null && now < cached.expiresAt()) {
            return cached.stats();
        }

        java.util.Map<String, Long> stats = java.util.Map.of(
                "total", complaintRepository.countBySocietyId(societyId),
                "open", complaintRepository.countBySocietyIdAndStatus(
                        societyId, ComplaintStatus.OPEN),
                "inProgress", complaintRepository.countBySocietyIdAndStatus(
                        societyId, ComplaintStatus.IN_PROGRESS),
                "resolved", complaintRepository.countBySocietyIdAndStatus(
                        societyId, ComplaintStatus.RESOLVED),
                "rejected", complaintRepository.countBySocietyIdAndStatus(
                        societyId, ComplaintStatus.REJECTED)
        );

        complaintStatsCache.put(societyId, new CachedComplaintStats(stats, now + STATS_TTL_MS));
        return stats;
    }

    private Complaint findComplaint(String complaintId) {
        String societyId = requireSocietyId();
        return complaintRepository.findByIdAndSocietyId(complaintId, societyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Complaint not found"));
    }

    private void validateAccess(Complaint complaint, User currentUser) {
        boolean isAdmin = currentUser.getRole() == Roles.ADMIN
                || currentUser.getRole() == Roles.SUPER_ADMIN;

        if (!java.util.Objects.equals(complaint.getSocietyId(), TenantContext.getSocietyId())
                || (!isAdmin && !java.util.Objects.equals(complaint.getResidentId(), currentUser.getId()))) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied");
        }
    }
}
