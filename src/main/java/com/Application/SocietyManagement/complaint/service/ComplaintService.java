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

    public ComplaintResponse create(CreateComplaintRequest request,
                                    User currentUser) {
        Complaint complaint = Complaint.builder()
                .societyId(TenantContext.getSocietyId())
                .residentId(currentUser.getId())
                .residentName(currentUser.getFirstName()
                        + " " + currentUser.getLastName())
                .apartmentNumber(currentUser.getFlatId())
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .status(ComplaintStatus.OPEN)
                .build();

        Complaint saved = complaintRepository.save(complaint);
        log.info("Complaint created by {} for society {}",
                currentUser.getEmail(), saved.getSocietyId());

        return ComplaintResponse.from(saved);
    }

    public PagedResponse<ComplaintResponse> getComplaints(
            ComplaintStatus status,
            ComplaintCategory category,
            int page, int size,
            User currentUser) {

        String societyId = TenantContext.getSocietyId();
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
            if (status != null) {
                result = complaintRepository
                        .findBySocietyIdAndResidentIdAndStatus(
                                societyId, currentUser.getId(),
                                status, pageable);
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
        Complaint complaint = findComplaint(complaintId);
        validateAccess(complaint, currentUser);
        return ComplaintResponse.from(complaint);
    }

    public ComplaintResponse updateStatus(String complaintId,
                                          UpdateComplaintRequest request,
                                          User currentUser) {
        Complaint complaint = findComplaint(complaintId);

        ComplaintStatus oldStatus = complaint.getStatus();
        complaint.setStatus(request.getStatus());

        if (request.getAdminNote() != null) {
            complaint.setAdminNote(request.getAdminNote());
        }

        if (request.getStatus() == ComplaintStatus.RESOLVED) {
            complaint.setResolvedBy(currentUser.getFirstName()
                    + " " + currentUser.getLastName());
        }

        Complaint saved = complaintRepository.save(complaint);
        log.info("Complaint {} status changed from {} to {} by {}",
                complaintId, oldStatus, request.getStatus(),
                currentUser.getEmail());

        return ComplaintResponse.from(saved);
    }

    public void delete(String complaintId, User currentUser) {
        Complaint complaint = findComplaint(complaintId);
        validateAccess(complaint, currentUser);

        if (complaint.getStatus() != ComplaintStatus.OPEN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only OPEN complaints can be deleted");
        }

        complaintRepository.delete(complaint);
        log.info("Complaint {} deleted by {}", complaintId,
                currentUser.getEmail());
    }

    // ── Dashboard stats ──

    public java.util.Map<String, Long> getStats() {
        String societyId = TenantContext.getSocietyId();
        return java.util.Map.of(
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
    }

    private Complaint findComplaint(String complaintId) {
        return complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Complaint not found"));
    }

    private void validateAccess(Complaint complaint, User currentUser) {
        boolean isAdmin = currentUser.getRole() == Roles.ADMIN
                || currentUser.getRole() == Roles.SUPER_ADMIN;

        if (!isAdmin && !complaint.getResidentId()
                .equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied");
        }
    }
}