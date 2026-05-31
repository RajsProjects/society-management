package com.Application.SocietyManagement.complaint.repository;

import com.Application.SocietyManagement.complaint.entity.Complaint;
import com.Application.SocietyManagement.complaint.enums.ComplaintCategory;
import com.Application.SocietyManagement.complaint.enums.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ComplaintRepository
        extends MongoRepository<Complaint, String> {

    Page<Complaint> findBySocietyId(
            String societyId, Pageable pageable);

    Page<Complaint> findBySocietyIdAndStatus(
            String societyId, ComplaintStatus status, Pageable pageable);

    Page<Complaint> findBySocietyIdAndCategory(
            String societyId, ComplaintCategory category, Pageable pageable);

    Page<Complaint> findBySocietyIdAndStatusAndCategory(
            String societyId, ComplaintStatus status,
            ComplaintCategory category, Pageable pageable);

    Page<Complaint> findBySocietyIdAndResidentId(
            String societyId, String residentId, Pageable pageable);

    Page<Complaint> findBySocietyIdAndResidentIdAndStatus(
            String societyId, String residentId,
            ComplaintStatus status, Pageable pageable);

    long countBySocietyId(String societyId);
    long countBySocietyIdAndStatus(String societyId, ComplaintStatus status);
}