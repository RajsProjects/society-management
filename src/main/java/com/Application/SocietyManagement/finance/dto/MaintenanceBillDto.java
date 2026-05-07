package com.Application.SocietyManagement.finance.dto;

import com.Application.SocietyManagement.finance.entity.MaintenanceBill;
import com.Application.SocietyManagement.finance.enums.BillStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class MaintenanceBillDto {

    private String id;
    private String apartmentNumber;
    private BigDecimal amount;
    private String billingMonth;
    private LocalDate dueDate;
    private BillStatus status;

    public static MaintenanceBillDto from(MaintenanceBill bill) {
        return MaintenanceBillDto.builder()
                .id(bill.getId())
                .apartmentNumber(bill.getApartmentNumber())
                .amount(bill.getAmount())
                .billingMonth(bill.getBillingMonth())
                .dueDate(bill.getDueDate())
                .status(bill.getStatus())
                .build();
    }
}
