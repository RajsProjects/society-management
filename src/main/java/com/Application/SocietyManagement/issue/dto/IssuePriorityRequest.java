package com.Application.SocietyManagement.issue.dto;

import com.Application.SocietyManagement.issue.enums.IssuePriority;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class IssuePriorityRequest {

    @NotNull(message = "Priority is required")
    private IssuePriority priority;
}