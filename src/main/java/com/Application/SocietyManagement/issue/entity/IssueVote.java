package com.Application.SocietyManagement.issue.entity;

import com.Application.SocietyManagement.core.common.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "issue_votes")
@CompoundIndex(def = "{'issueId': 1, 'userId': 1}", unique = true)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueVote extends BaseEntity {

    private String issueId;
    private String userId;
    private String societyId;
}
