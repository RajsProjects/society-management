package com.Application.SocietyManagement.finance.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CreateBillRequest {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Apartment number is required")
    private String apartmentNumber;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotBlank(message = "Billing month is required")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Billing month must be in YYYY-MM format")
    private String billingMonth;

    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    private LocalDate dueDate;
}
