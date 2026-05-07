package com.Application.SocietyManagement.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PayBillRequest {

    @NotBlank(message = "UPI transaction ID is required")
    private String upiTransactionId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;
}
