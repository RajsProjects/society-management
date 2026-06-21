package com.Application.SocietyManagement.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyPaymentRequest {
    @NotBlank
    private String razorpayOrderId;
    @NotBlank
    private String razorpayPaymentId;
    @NotBlank
    private String razorpaySignature;
}