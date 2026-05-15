package com.Application.SocietyManagement.finance.repository;

import com.Application.SocietyManagement.finance.entity.MaintenanceBill;
import com.Application.SocietyManagement.finance.enums.BillStatus;
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
}