package com.Application.SocietyManagement.subscription.entity;

import com.Application.SocietyManagement.core.common.BaseEntity;
import com.Application.SocietyManagement.society.enums.SubscriptionPlan;
import com.Application.SocietyManagement.society.enums.SubscriptionStatus;
import com.Application.SocietyManagement.society.enums.PaymentStatus;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "subscriptions")
@CompoundIndexes({
        @CompoundIndex(def = "{'societyId': 1, 'createdAt': -1}", name = "sub_society_created_idx"),
        @CompoundIndex(def = "{'razorpayOrderId': 1, 'societyId': 1}", name = "sub_order_society_idx")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription extends BaseEntity {
    private String societyId;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;

    private Instant startDate;
    private Instant endDate;

    private long amount;                  // paise

    @Builder.Default
    private String currency = "INR";

    @Indexed
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    private String failureReason;
    private Instant paidAt;
}