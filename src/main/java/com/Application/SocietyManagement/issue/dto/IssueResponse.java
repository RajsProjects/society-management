package com.Application.SocietyManagement.issue.dto;

import com.Application.SocietyManagement.issue.enums.IssuePriority;
import com.Application.SocietyManagement.issue.enums.IssueStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class IssueResponse {

    private String id;
    private String title;
    private String description;
    private String photoUrl;
    private IssueStatus status;
    private IssuePriority priority;
    private CreatorDto creator;
    private long voteCount;
    private Instant createdAt;
    private Instant updatedAt;
}
