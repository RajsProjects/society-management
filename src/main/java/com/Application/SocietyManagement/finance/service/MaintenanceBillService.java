package com.Application.SocietyManagement.finance.service;

import com.Application.SocietyManagement.communication.email.event.BillGeneratedEvent;
import com.Application.SocietyManagement.communication.email.event.PaymentSuccessEvent;
import com.Application.SocietyManagement.finance.dto.*;
import com.Application.SocietyManagement.finance.entity.MaintenanceBill;
import com.Application.SocietyManagement.finance.enums.BillStatus;
import com.Application.SocietyManagement.finance.repository.MaintenanceBillRepository;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.repository.UserRepository;
import com.Application.SocietyManagement.core.tenant.TenantContext;
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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.Application.SocietyManagement.core.util.DistributedLockService lockService;

    public MaintenanceBillDto createBill(CreateBillRequest request) {
        String societyId = requireSocietyId();
        User user = userRepository.findByIdAndSocietyId(request.getUserId(), societyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (user.getFlatId() == null || !user.getFlatId().equals(request.getApartmentNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Apartment number does not match user");
        }

        if (billRepository.existsByApartmentNumberAndBillingMonthAndSocietyId(
                request.getApartmentNumber(), request.getBillingMonth(), societyId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Bill already exists for this billing month");
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

        // publish event - email sent async, does not block response
        eventPublisher.publishEvent(new BillGeneratedEvent(this, saved, user));

        return MaintenanceBillDto.from(saved);
    }

    public PagedResponse<MaintenanceBillDto> getBills(User currentUser,
                                                      BillStatus status,
                                                      int page, int size) {
        String societyId = requireSocietyId();
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<MaintenanceBill> result;
        boolean isAdmin = currentUser.getRole() == Roles.ADMIN
                || currentUser.getRole() == Roles.SUPER_ADMIN;

        if (isAdmin && status != null) {
            result = billRepository.findByStatusAndSocietyId(status, societyId, pageable);
        } else if (isAdmin) {
            result = billRepository.findBySocietyId(societyId, pageable);
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

    public MaintenanceBillDto getBillById(String billId, User currentUser) {
        String societyId = requireSocietyId();
        MaintenanceBill bill = billRepository.findByIdAndSocietyId(billId, societyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bill not found"));

        boolean isAdmin = currentUser.getRole() == Roles.ADMIN || currentUser.getRole() == Roles.SUPER_ADMIN;
        if (!isAdmin && !bill.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return MaintenanceBillDto.from(bill);
    }

    private static final java.util.regex.Pattern UPI_TXN_PATTERN =
            java.util.regex.Pattern.compile("^[A-Za-z0-9_-]{10,50}$");

    public Map<String, String> payBill(String billId, User currentUser,
                                       PayBillRequest request) {
        String societyId = requireSocietyId();
        MaintenanceBill bill = billRepository.findByIdAndSocietyId(billId, societyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bill not found"));

        // ownership check
        if (!bill.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Access denied");
        }

        if (bill.getStatus() == BillStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Bill is already paid");
        }

        if (bill.getStatus() == BillStatus.PENDING_VERIFICATION) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Payment reference is already submitted and pending verification");
        }

        if (bill.getStatus() == BillStatus.OVERDUE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Overdue bills require admin intervention");
        }

        // full payment check
        if (request.getAmount().compareTo(bill.getAmount()) != 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only full payments are accepted");
        }

        // Strict Financial Fraud Prevention: Transaction ID format & anti-spoofing
        String txnId = request.getUpiTransactionId() != null ? request.getUpiTransactionId().trim() : "";
        if (!UPI_TXN_PATTERN.matcher(txnId).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid UPI transaction reference. Must be a valid 10-50 alphanumeric reference number.");
        }

        // Anti-Fraud: duplicate transaction prevention across all society bills
        if (billRepository.existsByUpiTransactionId(txnId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This transaction reference has already been submitted for another payment. Duplicate submission detected.");
        }

        // Concurrency lock: prevent race conditions on payment processing
        String lockKey = "bill:pay:" + billId;
        boolean locked = lockService == null || lockService.tryLock(lockKey, java.time.Duration.ofSeconds(15));
        if (!locked) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment processing already in progress for this bill. Please wait.");
        }

        try {
            bill.setStatus(BillStatus.PENDING_VERIFICATION);
            bill.setUpiTransactionId(txnId);
            billRepository.save(bill);

            return Map.of(
                    "message", "Payment reference submitted successfully. Verification pending by management.",
                    "status", BillStatus.PENDING_VERIFICATION.name(),
                    "upiTransactionId", txnId
            );
        } finally {
            if (lockService != null) {
                lockService.releaseLock(lockKey);
            }
        }
    }

    public MaintenanceBillDto verifyPayment(String billId, User currentUser) {
        String societyId = requireSocietyId();
        MaintenanceBill bill = billRepository.findByIdAndSocietyId(billId, societyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bill not found"));

        if (bill.getStatus() != BillStatus.PENDING_VERIFICATION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Bill is not awaiting verification (current status: " + bill.getStatus() + ")");
        }

        bill.setStatus(BillStatus.PAID);
        bill.setPaidAt(java.time.Instant.now());
        MaintenanceBill saved = billRepository.save(bill);

        User resident = userRepository.findById(saved.getUserId()).orElse(null);
        eventPublisher.publishEvent(new PaymentSuccessEvent(this, saved, resident));

        return MaintenanceBillDto.from(saved);
    }

    public MaintenanceBillDto rejectPayment(String billId, User currentUser, String rejectionReason) {
        String societyId = requireSocietyId();
        MaintenanceBill bill = billRepository.findByIdAndSocietyId(billId, societyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bill not found"));

        if (bill.getStatus() != BillStatus.PENDING_VERIFICATION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Bill is not awaiting verification (current status: " + bill.getStatus() + ")");
        }

        BillStatus revertedStatus = bill.getDueDate().isBefore(LocalDate.now()) ? BillStatus.OVERDUE : BillStatus.PENDING;
        bill.setStatus(revertedStatus);
        bill.setUpiTransactionId(null);
        MaintenanceBill saved = billRepository.save(bill);

        log.warn("Payment rejected for bill {} by user {}. Reason: {}. Reverted to status: {}",
                billId, currentUser.getEmail(), rejectionReason, revertedStatus);

        return MaintenanceBillDto.from(saved);
    }

    // runs at midnight UTC every day
    @Scheduled(cron = "0 0 0 * * *", zone = "UTC")
    public void markOverdueBills() {
        if (lockService != null && !lockService.tryLock("markOverdueBills", java.time.Duration.ofMinutes(15))) {
            log.info("Another cluster node is currently executing markOverdueBills. Skipping on this node.");
            return;
        }
        if (mongoTemplate != null) {
            org.springframework.data.mongodb.core.query.Query query =
                    org.springframework.data.mongodb.core.query.Query.query(
                            org.springframework.data.mongodb.core.query.Criteria.where("status").is(BillStatus.PENDING)
                                    .and("dueDate").lt(LocalDate.now())
                    );
            org.springframework.data.mongodb.core.query.Update update =
                    new org.springframework.data.mongodb.core.query.Update()
                            .set("status", BillStatus.OVERDUE)
                            .set("updatedAt", java.time.Instant.now());

            com.mongodb.client.result.UpdateResult result =
                    mongoTemplate.updateMulti(query, update, MaintenanceBill.class);
            log.info("Marked {} bills as OVERDUE via atomic updateMulti", result.getModifiedCount());
        } else {
            List<MaintenanceBill> overdue = billRepository
                    .findByStatusAndDueDateBefore(BillStatus.PENDING, LocalDate.now());

            if (overdue.isEmpty()) return;

            overdue.forEach(bill -> bill.setStatus(BillStatus.OVERDUE));
            billRepository.saveAll(overdue);
            log.info("Marked {} bills as OVERDUE", overdue.size());
        }
    }

    private String requireSocietyId() {
        String societyId = TenantContext.getSocietyId();
        if (societyId == null || societyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No society context");
        }
        return societyId;
    }
}
