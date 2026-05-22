package com.Application.SocietyManagement.issue.entity;

import com.Application.SocietyManagement.core.common.BaseEntity;
import com.Application.SocietyManagement.issue.enums.IssuePriority;
import com.Application.SocietyManagement.issue.enums.IssueStatus;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "issues")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Issue extends BaseEntity {

    private String title;
    private String description;
    private String photoUrl;

    @Builder.Default
    private IssueStatus status = IssueStatus.OPEN;

    @Builder.Default
    private IssuePriority priority = IssuePriority.LOW;

    private String creatorId;
    private String societyId;
}
