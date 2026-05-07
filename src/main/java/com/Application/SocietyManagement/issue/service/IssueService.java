package com.Application.SocietyManagement.issue.service;

import com.Application.SocietyManagement.issue.dto.*;
import com.Application.SocietyManagement.issue.entity.Issue;
import com.Application.SocietyManagement.issue.entity.IssueVote;
import com.Application.SocietyManagement.issue.enums.IssueStatus;
import com.Application.SocietyManagement.issue.repository.IssueRepository;
import com.Application.SocietyManagement.issue.repository.IssueVoteRepository;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final IssueVoteRepository issueVoteRepository;
    private final UserRepository userRepository;

    public IssueResponse createIssue(IssueRequest request, String creatorId) {
        Issue issue = Issue.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .photoUrl(request.getPhotoUrl())
                .creatorId(creatorId)
                .build();

        Issue saved = issueRepository.save(issue);
        return toResponse(saved);
    }

    public PagedResponse<IssueResponse> getIssues(IssueStatus status,
                                                  String sortBy,
                                                  String direction,
                                                  int page, int size) {
        Sort.Direction dir = direction.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        // voteCount sorting handled in memory since it's a derived field
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(dir, "createdAt"));

        Page<Issue> result = status != null
                ? issueRepository.findByStatus(status, pageable)
                : issueRepository.findAll(pageable);

        List<IssueResponse> content = result.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        // sort by voteCount if requested
        if ("voteCount".equalsIgnoreCase(sortBy)) {
            content = content.stream()
                    .sorted((a, b) -> dir == Sort.Direction.DESC
                            ? Long.compare(b.getVoteCount(), a.getVoteCount())
                            : Long.compare(a.getVoteCount(), b.getVoteCount()))
                    .toList();
        }

        return PagedResponse.<IssueResponse>builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    public IssueResponse updateStatus(String issueId, IssueStatus status) {
        Issue issue = findIssueById(issueId);
        issue.setStatus(status);
        return toResponse(issueRepository.save(issue));
    }

    public IssueResponse updatePriority(String issueId,
                                        com.Application.SocietyManagement.issue.enums.IssuePriority priority) {
        Issue issue = findIssueById(issueId);
        issue.setPriority(priority);
        return toResponse(issueRepository.save(issue));
    }

    public void addVote(String issueId, String userId) {
        Issue issue = findIssueById(issueId);

        if (issue.getCreatorId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot vote on your own issue");
        }

        if (issueVoteRepository.existsByIssueIdAndUserId(issueId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Already voted on this issue");
        }

        issueVoteRepository.save(IssueVote.builder()
                .issueId(issueId)
                .userId(userId)
                .build());
    }

    public void removeVote(String issueId, String userId) {
        IssueVote vote = issueVoteRepository
                .findByIssueIdAndUserId(issueId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vote not found"));

        issueVoteRepository.delete(vote);
    }

    private Issue findIssueById(String issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Issue not found"));
    }

    private IssueResponse toResponse(Issue issue) {
        User creator = userRepository.findById(issue.getCreatorId())
                .orElse(null);

        CreatorDto creatorDto = creator != null
                ? CreatorDto.builder()
                  .id(creator.getId())
                  .firstName(creator.getFirstName())
                  .lastName(creator.getLastName())
                  .build()
                : null;

        return IssueResponse.builder()
                .id(issue.getId())
                .title(issue.getTitle())
                .description(issue.getDescription())
                .photoUrl(issue.getPhotoUrl())
                .status(issue.getStatus())
                .priority(issue.getPriority())
                .creator(creatorDto)
                .voteCount(issueVoteRepository.countByIssueId(issue.getId()))
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .build();
    }
}
