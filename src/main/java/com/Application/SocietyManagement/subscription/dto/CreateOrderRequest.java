package com.Application.SocietyManagement.subscription.dto;

import com.Application.SocietyManagement.society.enums.SubscriptionPlan;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {
    @NotNull(message = "Plan is required")
    private SubscriptionPlan plan;
}