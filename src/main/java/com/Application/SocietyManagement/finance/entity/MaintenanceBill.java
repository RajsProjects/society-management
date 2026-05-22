package com.Application.SocietyManagement.finance.entity;

import com.Application.SocietyManagement.finance.enums.BillStatus;
import com.Application.SocietyManagement.core.common.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;


@Document(collection = "maintenance_bills")
@CompoundIndex(def = "{'apartmentNumber': 1, 'billingMonth': 1}", unique = true)
@Builder
@Getter
@Setter
public class MaintenanceBill extends BaseEntity {

    private String userId;
    private String apartmentNumber;
    private BigDecimal amount;
    private String billingMonth;
    private LocalDate dueDate;

    @Builder.Default
    private BillStatus status = BillStatus.PENDING;

    private String upiTransactionId;
    private String societyId;
}