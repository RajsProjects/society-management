package com.Application.SocietyManagement.issue.dto;

import com.Application.SocietyManagement.issue.enums.IssueStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class IssueStatusRequest {

    @NotNull(message = "Status is required")
    private IssueStatus status;
}
