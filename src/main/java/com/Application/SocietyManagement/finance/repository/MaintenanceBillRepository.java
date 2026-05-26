package com.Application.SocietyManagement.finance.repository;

import com.Application.SocietyManagement.finance.entity.MaintenanceBill;
import com.Application.SocietyManagement.finance.enums.BillStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface MaintenanceBillRepository
        extends MongoRepository<MaintenanceBill, String> {

    // old methods — keep for scheduler
    List<MaintenanceBill> findByStatusAndDueDateBefore(
            BillStatus status, LocalDate date);

    // new society-scoped methods
    boolean existsByApartmentNumberAndBillingMonthAndSocietyId(
            String apartmentNumber, String billingMonth, String societyId);

    Page<MaintenanceBill> findBySocietyId(
            String societyId, Pageable pageable);

    Page<MaintenanceBill> findByStatusAndSocietyId(
            BillStatus status, String societyId, Pageable pageable);

    Page<MaintenanceBill> findByUserIdAndSocietyId(
            String userId, String societyId, Pageable pageable);

    Page<MaintenanceBill> findByUserIdAndStatusAndSocietyId(
            String userId, BillStatus status,
            String societyId, Pageable pageable);

    List<MaintenanceBill> findByStatusAndDueDateAndSocietyId(
            BillStatus status, LocalDate date, String societyId);
}