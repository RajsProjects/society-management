package com.Application.SocietyManagement.society.dto;

import com.Application.SocietyManagement.society.enums.SocietyStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifySocietyRequest {

    @NotNull(message = "Status is required")
    private SocietyStatus status; // ACTIVE or REJECTED

    private String rejectionReason; // required if REJECTED
}
