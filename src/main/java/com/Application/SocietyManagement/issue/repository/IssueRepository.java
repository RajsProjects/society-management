package com.Application.SocietyManagement.issue.repository;

import com.Application.SocietyManagement.issue.entity.Issue;
import com.Application.SocietyManagement.issue.enums.IssueStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IssueRepository extends MongoRepository<Issue, String> {
    Page<Issue> findByStatus(IssueStatus status, Pageable pageable);
    Page<Issue> findBySocietyId(String societyId, Pageable pageable);
    Page<Issue> findByStatusAndSocietyId(IssueStatus status, String societyId, Pageable pageable);

}
