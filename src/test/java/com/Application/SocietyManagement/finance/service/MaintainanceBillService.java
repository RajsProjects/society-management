package com.Application.SocietyManagement.users.finance.service;

import com.Application.SocietyManagement.finance.dto.CreateBillRequest;
import com.Application.SocietyManagement.finance.dto.MaintenanceBillDto;
import com.Application.SocietyManagement.finance.dto.PayBillRequest;
import com.Application.SocietyManagement.finance.entity.MaintenanceBill;
import com.Application.SocietyManagement.finance.enums.BillStatus;
import com.Application.SocietyManagement.finance.repository.MaintenanceBillRepository;
import com.Application.SocietyManagement.finance.service.MaintenanceBillService;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceBillServiceTest {

    @Mock private MaintenanceBillRepository billRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private MaintenanceBillService billService;

    private User residentUser;
    private User adminUser;
    private MaintenanceBill pendingBill;
    private CreateBillRequest createRequest;
    private PayBillRequest payRequest;

    @BeforeEach
    void setUp() {
        residentUser = User.builder()
                .email("resident@test.com")
                .apartmentNumber("A-101")
                .role(Roles.RESIDENT)
                .status(Status.ACTIVE)
                .build();

        adminUser = User.builder()
                .email("admin@test.com")
                .role(Roles.ADMIN)
                .status(Status.ACTIVE)
                .build();

        pendingBill = MaintenanceBill.builder()
                .userId("resident123")
                .apartmentNumber("A-101")
                .amount(new BigDecimal("1500.00"))
                .billingMonth("2026-05")
                .dueDate(LocalDate.now().plusDays(10))
                .status(BillStatus.PENDING)
                .build();

        createRequest = new CreateBillRequest();
        createRequest.setUserId("resident123");
        createRequest.setApartmentNumber("A-101");
        createRequest.setAmount(new BigDecimal("1500.00"));
        createRequest.setBillingMonth("2026-05");
        createRequest.setDueDate(LocalDate.now().plusDays(10));

        payRequest = new PayBillRequest();
        payRequest.setUpiTransactionId("UPI1234567890");
        payRequest.setAmount(new BigDecimal("1500.00"));
    }

    // ── createBill tests ──

    @Test
    void createBill_success_returnsPendingBill() {
        when(userRepository.findById("resident123"))
                .thenReturn(Optional.of(residentUser));
        when(billRepository.existsByApartmentNumberAndBillingMonth("A-101", "2026-05"))
                .thenReturn(false);
        when(billRepository.save(any(MaintenanceBill.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MaintenanceBillDto result = billService.createBill(createRequest);

        assertThat(result.getApartmentNumber()).isEqualTo("A-101");
        assertThat(result.getAmount()).isEqualByComparingTo("1500.00");
        assertThat(result.getStatus()).isEqualTo(BillStatus.PENDING);
    }

    @Test
    void createBill_userNotFound_throwsNotFound() {
        when(userRepository.findById("resident123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billService.createBill(createRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User not found");

        verify(billRepository, never()).save(any());
    }

    @Test
    void createBill_apartmentMismatch_throwsBadRequest() {
        residentUser.setApartmentNumber("B-202");
        when(userRepository.findById("resident123"))
                .thenReturn(Optional.of(residentUser));

        assertThatThrownBy(() -> billService.createBill(createRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Apartment number does not match user");

        verify(billRepository, never()).save(any());
    }

    @Test
    void createBill_duplicateBillingMonth_throwsConflict() {
        when(userRepository.findById("resident123"))
                .thenReturn(Optional.of(residentUser));
        when(billRepository.existsByApartmentNumberAndBillingMonth("A-101", "2026-05"))
                .thenReturn(true);

        assertThatThrownBy(() -> billService.createBill(createRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Bill already exists for this billing month");

        verify(billRepository, never()).save(any());
    }

    // ── getBills tests ──

    @Test
    void getBills_adminNoFilter_returnsAllBills() {
        Page<MaintenanceBill> page = new PageImpl<>(List.of(pendingBill));
        when(billRepository.findAll(any(Pageable.class))).thenReturn(page);

        PagedResponse<MaintenanceBillDto> result =
                billService.getBills(adminUser, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        verify(billRepository).findAll(any(Pageable.class));
    }

    @Test
    void getBills_adminWithStatusFilter_filtersByStatus() {
        Page<MaintenanceBill> page = new PageImpl<>(List.of(pendingBill));
        when(billRepository.findByStatus(eq(BillStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<MaintenanceBillDto> result =
                billService.getBills(adminUser, BillStatus.PENDING, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        verify(billRepository).findByStatus(eq(BillStatus.PENDING), any(Pageable.class));
        verify(billRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getBills_residentNoFilter_returnsOwnBillsOnly() {
        Page<MaintenanceBill> page = new PageImpl<>(List.of(pendingBill));
        when(billRepository.findByUserId(eq(residentUser.getId()), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<MaintenanceBillDto> result =
                billService.getBills(residentUser, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        verify(billRepository).findByUserId(eq(residentUser.getId()), any(Pageable.class));
        verify(billRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getBills_residentWithStatusFilter_returnsOwnBillsByStatus() {
        Page<MaintenanceBill> page = new PageImpl<>(List.of(pendingBill));
        when(billRepository.findByUserIdAndStatus(
                eq(residentUser.getId()), eq(BillStatus.PENDING), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<MaintenanceBillDto> result =
                billService.getBills(residentUser, BillStatus.PENDING, 0, 20);

        verify(billRepository).findByUserIdAndStatus(
                eq(residentUser.getId()), eq(BillStatus.PENDING), any(Pageable.class));
    }

    // ── payBill tests ──

    @Test
    void payBill_success_marksBillAsPaid() {
        residentUser = User.builder()
                .email("resident@test.com")
                .apartmentNumber("A-101")
                .role(Roles.RESIDENT)
                .status(Status.ACTIVE)
                .build();
        ReflectionTestUtils.setField(residentUser, "id", "resident123");
        pendingBill.setUserId("resident123");

        when(billRepository.findById("bill1")).thenReturn(Optional.of(pendingBill));
        when(billRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = billService.payBill("bill1", residentUser, payRequest);

        assertThat(result).containsEntry("message", "Payment successful");
        assertThat(result).containsEntry("status", "PAID");
        assertThat(pendingBill.getStatus()).isEqualTo(BillStatus.PAID);
        assertThat(pendingBill.getUpiTransactionId()).isEqualTo("UPI1234567890");
    }

    @Test
    void payBill_billNotFound_throwsNotFound() {
        when(billRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                billService.payBill("nonexistent", residentUser, payRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Bill not found");
    }

    @Test
    void payBill_notOwner_throwsForbidden() {
        pendingBill.setUserId("anotherUser");
        when(billRepository.findById("bill1")).thenReturn(Optional.of(pendingBill));

        assertThatThrownBy(() ->
                billService.payBill("bill1", residentUser, payRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Access denied");

        verify(billRepository, never()).save(any());
    }

    @Test
    void payBill_alreadyPaid_throwsConflict() {
        ReflectionTestUtils.setField(residentUser, "id", "resident123");
        pendingBill.setUserId("resident123");
        pendingBill.setStatus(BillStatus.PAID);
        when(billRepository.findById("bill1")).thenReturn(Optional.of(pendingBill));

        assertThatThrownBy(() ->
                billService.payBill("bill1", residentUser, payRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already paid");

        verify(billRepository, never()).save(any());
    }

    @Test
    void payBill_overdueBill_throwsForbidden() {
        ReflectionTestUtils.setField(residentUser, "id", "resident123");
        pendingBill.setUserId("resident123");
        pendingBill.setStatus(BillStatus.OVERDUE);
        when(billRepository.findById("bill1")).thenReturn(Optional.of(pendingBill));

        assertThatThrownBy(() ->
                billService.payBill("bill1", residentUser, payRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("admin intervention");

        verify(billRepository, never()).save(any());
    }

    @Test
    void payBill_partialPayment_throwsBadRequest() {
        ReflectionTestUtils.setField(residentUser, "id", "resident123");
        pendingBill.setUserId("resident123");
        payRequest.setAmount(new BigDecimal("500.00"));
        when(billRepository.findById("bill1")).thenReturn(Optional.of(pendingBill));

        assertThatThrownBy(() ->
                billService.payBill("bill1", residentUser, payRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only full payments are accepted");

        verify(billRepository, never()).save(any());
    }

    // ── markOverdueBills tests ──

    @Test
    void markOverdueBills_pendingBillsPastDue_markedOverdue() {
        MaintenanceBill overdueBill = MaintenanceBill.builder()
                .userId("resident123")
                .apartmentNumber("A-101")
                .amount(new BigDecimal("1500.00"))
                .billingMonth("2026-03")
                .dueDate(LocalDate.now().minusDays(5))
                .status(BillStatus.PENDING)
                .build();

        when(billRepository.findByStatusAndDueDateBefore(
                eq(BillStatus.PENDING), any(LocalDate.class)))
                .thenReturn(List.of(overdueBill));

        billService.markOverdueBills();

        assertThat(overdueBill.getStatus()).isEqualTo(BillStatus.OVERDUE);
        verify(billRepository).saveAll(List.of(overdueBill));
    }

    @Test
    void markOverdueBills_noBillsDue_doesNotSave() {
        when(billRepository.findByStatusAndDueDateBefore(
                eq(BillStatus.PENDING), any(LocalDate.class)))
                .thenReturn(List.of());

        billService.markOverdueBills();

        verify(billRepository, never()).saveAll(any());
    }
}
