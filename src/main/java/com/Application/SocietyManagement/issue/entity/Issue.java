package com.Application.SocietyManagement.issue.entity;

import com.Application.SocietyManagement.core.common.BaseEntity;
import com.Application.SocietyManagement.issue.enums.IssuePriority;
import com.Application.SocietyManagement.issue.enums.IssueStatus;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "issues")
@CompoundIndexes({
        @CompoundIndex(def = "{'societyId': 1, 'createdAt': -1}", name = "issue_society_created_idx"),
        @CompoundIndex(def = "{'societyId': 1, 'status': 1, 'createdAt': -1}", name = "issue_society_status_created_idx")
})
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
