package com.Application.SocietyManagement.society.entity;

import com.Application.SocietyManagement.core.common.BaseEntity;
import com.Application.SocietyManagement.society.enums.SocietyStatus;
import com.Application.SocietyManagement.society.enums.SubscriptionPlan;
import com.Application.SocietyManagement.society.enums.SubscriptionStatus;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "societies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Society extends BaseEntity {

    // Identity
    private String name;

    @Indexed(unique = true)
    private String registrationNumber;    // RWA/Housing Society number

    private String address;
    private String city;
    private String state;
    private String pincode;
    private Integer totalFlats;
    private String adminEmail;

    // Verification
    @Builder.Default
    private SocietyStatus status =
            SocietyStatus.PENDING_VERIFICATION;
    private String documentUrl;           // S3 URL
    private String rejectionReason;
    private Instant verifiedAt;
    private String verifiedBy;            // platformAdmin userId

    // Access
    @Indexed(unique = true)
    private String joinCode;              // SOC-KD8X72 permanent

    private String superAdminId;          // creator userId

    @Indexed(unique = true)
    private String societyCode;           // display code

    // Subscription
    @Builder.Default
    private SubscriptionPlan plan =
            SubscriptionPlan.TRIAL;

    @Builder.Default
    private SubscriptionStatus subscriptionStatus =
            SubscriptionStatus.TRIAL;

    private Instant trialEndsAt;
    private Instant subscriptionEndsAt;
    private String razorpayCustomerId;
    private String razorpaySubscriptionId;

    // Stats (denormalized)
    @Builder.Default
    private Integer activeResidents = 0;

    @Builder.Default
    private Integer occupiedFlats = 0;
}