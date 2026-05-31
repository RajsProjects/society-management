package com.Application.SocietyManagement.finance.repository;

import com.Application.SocietyManagement.finance.entity.MaintenanceBill;
import com.Application.SocietyManagement.finance.enums.BillStatus;
import com.Application.SocietyManagement.issue.entity.Issue;
import com.Application.SocietyManagement.issue.enums.IssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface MaintenanceBillRepository extends MongoRepository<MaintenanceBill, String> {
    boolean existsByApartmentNumberAndBillingMonth(String apartmentNumber, String billingMonth);
    Page<MaintenanceBill> findByUserId(String userId, Pageable pageable);
    Page<MaintenanceBill> findByStatus(BillStatus status, Pageable pageable);
    Page<MaintenanceBill> findByUserIdAndStatus(String userId, BillStatus status, Pageable pageable);
    List<MaintenanceBill> findByStatusAndDueDateBefore(BillStatus status, LocalDate date);
    List<MaintenanceBill> findByStatusAndDueDate(BillStatus status, LocalDate dueDate);
    Page<MaintenanceBill> findBySocietyId(String societyId, Pageable pageable);
    Page<MaintenanceBill> findByUserIdAndSocietyId(String userId, String societyId, Pageable pageable);
    long countBySocietyId(String societyId);
    long countBySocietyIdAndStatus(String societyId, BillStatus status);
}