package com.Application.SocietyManagement.complaint.dto;

import com.Application.SocietyManagement.complaint.enums.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateComplaintRequest {

    @NotNull(message = "Status is required")
    private ComplaintStatus status;

    private String adminNote;
}