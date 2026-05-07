package com.Application.SocietyManagement.users.dto;

import com.Application.SocietyManagement.users.enums.Status;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateStatusRequest {

    @NotNull(message = "Status is required")
    private Status status;
}
