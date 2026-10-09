package com.Application.SocietyManagement.finance.entity;

import com.Application.SocietyManagement.finance.enums.BillStatus;
import com.Application.SocietyManagement.core.common.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;


@Document(collection = "maintenance_bills")
@CompoundIndexes({
        @CompoundIndex(def = "{'societyId': 1, 'apartmentNumber': 1, 'billingMonth': 1}", unique = true),
        @CompoundIndex(def = "{'societyId': 1, 'userId': 1, 'status': 1, 'createdAt': -1}", name = "bill_society_user_status_idx"),
        @CompoundIndex(def = "{'societyId': 1, 'status': 1, 'createdAt': -1}", name = "bill_society_status_idx"),
        @CompoundIndex(def = "{'societyId': 1, 'createdAt': -1}", name = "bill_society_created_idx"),
        @CompoundIndex(def = "{'status': 1, 'dueDate': 1}", name = "bill_status_duedate_idx"),
        @CompoundIndex(def = "{'upiTransactionId': 1}", unique = true, sparse = true, name = "bill_upi_txn_unique_idx")
})
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
    private java.time.Instant paidAt;
    private String societyId;
}