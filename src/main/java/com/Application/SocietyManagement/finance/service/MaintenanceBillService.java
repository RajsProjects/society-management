package com.Application.SocietyManagement.finance.service;

import com.Application.SocietyManagement.communication.email.event.BillGeneratedEvent;
import com.Application.SocietyManagement.communication.email.event.PaymentSuccessEvent;
import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.finance.dto.*;
import com.Application.SocietyManagement.finance.entity.MaintenanceBill;
import com.Application.SocietyManagement.finance.enums.BillStatus;
import com.Application.SocietyManagement.finance.repository.MaintenanceBillRepository;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceBillService {

    private final MaintenanceBillRepository billRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MaintenanceBillDto createBill(CreateBillRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (!user.getApartmentNumber().equals(request.getApartmentNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Apartment number does not match user");
        }

        String societyId = TenantContext.getSocietyId();

        if (billRepository.existsByApartmentNumberAndBillingMonthAndSocietyId(
                request.getApartmentNumber(),
                request.getBillingMonth(),
                societyId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Bill already exists for this billing month");
        }

        MaintenanceBill bill = MaintenanceBill.builder()
                .userId(request.getUserId())
                .apartmentNumber(request.getApartmentNumber())
                .amount(request.getAmount())
                .billingMonth(request.getBillingMonth())
                .dueDate(request.getDueDate())
                .societyId(societyId)
                .build();

        MaintenanceBill saved = billRepository.save(bill);
        eventPublisher.publishEvent(new BillGeneratedEvent(this, saved, user));
        return MaintenanceBillDto.from(saved);
    }

    public PagedResponse<MaintenanceBillDto> getBills(User currentUser,
                                                      BillStatus status,
                                                      int page, int size) {
        String societyId = TenantContext.getSocietyId();
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<MaintenanceBill> result;
        boolean isAdmin = currentUser.getRole() == Roles.ADMIN;

        if (isAdmin && status != null) {
            result = billRepository.findByStatusAndSocietyId(
                    status, societyId, pageable);
        } else if (isAdmin) {
            result = billRepository.findBySocietyId(
                    societyId, pageable);
        } else if (status != null) {
            result = billRepository.findByUserIdAndStatusAndSocietyId(
                    currentUser.getId(), status, societyId, pageable);
        } else {
            result = billRepository.findByUserIdAndSocietyId(
                    currentUser.getId(), societyId, pageable);
        }

        return PagedResponse.<MaintenanceBillDto>builder()
                .content(result.getContent().stream()
                        .map(MaintenanceBillDto::from)
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    public Map<String, String> payBill(String billId, User currentUser,
                                       PayBillRequest request) {
        MaintenanceBill bill = billRepository.findById(billId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bill not found"));

        if (!bill.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied");
        }

        if (bill.getStatus() == BillStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Bill is already paid");
        }

        if (bill.getStatus() == BillStatus.OVERDUE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Overdue bills require admin intervention");
        }

        if (request.getAmount().compareTo(bill.getAmount()) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only full payments are accepted");
        }

        bill.setStatus(BillStatus.PAID);
        bill.setUpiTransactionId(request.getUpiTransactionId());
        billRepository.save(bill);

        eventPublisher.publishEvent(
                new PaymentSuccessEvent(this, bill, currentUser));

        return Map.of(
                "message", "Payment successful",
                "status", "PAID"
        );
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void markOverdueBills() {
        List<MaintenanceBill> overdue = billRepository
                .findByStatusAndDueDateBefore(
                        BillStatus.PENDING, LocalDate.now());

        if (overdue.isEmpty()) return;

        overdue.forEach(bill -> bill.setStatus(BillStatus.OVERDUE));
        billRepository.saveAll(overdue);
        log.info("Marked {} bills as OVERDUE", overdue.size());
    }
}