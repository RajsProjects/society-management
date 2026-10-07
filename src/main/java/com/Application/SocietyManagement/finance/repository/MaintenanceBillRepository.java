package com.Application.SocietyManagement.finance.repository;

import com.Application.SocietyManagement.finance.entity.MaintenanceBill;
import com.Application.SocietyManagement.finance.enums.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface MaintenanceBillRepository extends MongoRepository<MaintenanceBill, String> {
    @Deprecated
    boolean existsByApartmentNumberAndBillingMonth(String apartmentNumber, String billingMonth);
    @Deprecated
    Page<MaintenanceBill> findByUserId(String userId, Pageable pageable);
    @Deprecated
    Page<MaintenanceBill> findByStatus(BillStatus status, Pageable pageable);
    @Deprecated
    Page<MaintenanceBill> findByUserIdAndStatus(String userId, BillStatus status, Pageable pageable);
    boolean existsByApartmentNumberAndBillingMonthAndSocietyId(String apartmentNumber, String billingMonth, String societyId);
    Page<MaintenanceBill> findByUserIdAndSocietyId(String userId, String societyId, Pageable pageable);
    Page<MaintenanceBill> findByStatusAndSocietyId(BillStatus status, String societyId, Pageable pageable);
    Page<MaintenanceBill> findByUserIdAndStatusAndSocietyId(String userId, BillStatus status, String societyId, Pageable pageable);
    List<MaintenanceBill> findByStatusAndDueDateBefore(BillStatus status, LocalDate date);
    List<MaintenanceBill> findByStatusAndDueDate(BillStatus status, LocalDate dueDate);
    Page<MaintenanceBill> findBySocietyId(String societyId, Pageable pageable);
    java.util.Optional<MaintenanceBill> findByIdAndSocietyId(String id, String societyId);
    long countBySocietyId(String societyId);
    long countBySocietyIdAndStatus(String societyId, BillStatus status);
}