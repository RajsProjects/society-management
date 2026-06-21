package com.Application.SocietyManagement.subscription.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class OrderResponse {
    private String subscriptionId;   // our internal record id, client echoes back on verify
    private String razorpayOrderId;
    private long amount;             // paise
    private String currency;
    private String keyId;            // public key — client needs this to open Checkout
}