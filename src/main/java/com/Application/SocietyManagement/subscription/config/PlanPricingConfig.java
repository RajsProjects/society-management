package com.Application.SocietyManagement.subscription.config;

import com.Application.SocietyManagement.society.enums.SubscriptionPlan;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "razorpay.plans")
@Getter
@Setter
public class PlanPricingConfig {
    private long basic;
    private long growth;
    private long scale;

    public long getAmountFor(SubscriptionPlan plan) {
        return switch (plan) {
            case BASIC -> basic;
            case GROWTH -> growth;
            case SCALE -> scale;
            default -> throw new IllegalArgumentException(
                    "Plan " + plan + " is not self-serve. Contact sales for ENTERPRISE/TRIAL pricing.");
        };
    }
}