package com.Application.SocietyManagement.issue.repository;

import com.Application.SocietyManagement.issue.dto.IssueVoteCount;
import com.Application.SocietyManagement.issue.entity.IssueVote;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface IssueVoteRepository extends MongoRepository<IssueVote, String> {
    boolean existsByIssueIdAndUserId(String issueId, String userId);
    Optional<IssueVote> findByIssueIdAndUserId(String issueId, String userId);
    long countByIssueId(String issueId);
    void deleteByIssueId(String issueId);

    @Aggregation(pipeline = {
        "{ $match: { issueId: { $in: ?0 } } }",
        "{ $group: { _id: '$issueId', count: { $sum: 1 } } }"
    })
    List<IssueVoteCount> countVotesByIssueIds(List<String> issueIds);
}

