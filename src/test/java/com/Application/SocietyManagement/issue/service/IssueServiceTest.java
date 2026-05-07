package com.Application.SocietyManagement.issue.service;

import com.Application.SocietyManagement.issue.dto.IssueRequest;
import com.Application.SocietyManagement.issue.dto.IssueResponse;
import com.Application.SocietyManagement.issue.entity.Issue;
import com.Application.SocietyManagement.issue.entity.IssueVote;
import com.Application.SocietyManagement.issue.enums.IssuePriority;
import com.Application.SocietyManagement.issue.enums.IssueStatus;
import com.Application.SocietyManagement.issue.repository.IssueRepository;
import com.Application.SocietyManagement.issue.repository.IssueVoteRepository;
import com.Application.SocietyManagement.issue.service.IssueService;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock private IssueRepository issueRepository;
    @Mock private IssueVoteRepository issueVoteRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private IssueService issueService;

    private Issue issue;
    private User creator;
    private IssueRequest issueRequest;

    @BeforeEach
    void setUp() {
        creator = User.builder()
                .email("resident@test.com")
                .firstName("John")
                .lastName("Doe")
                .role(Roles.RESIDENT)
                .status(Status.ACTIVE)
                .build();

        issue = Issue.builder()
                .title("Broken elevator")
                .description("Elevator stuck on 3rd floor")
                .creatorId("creator123")
                .build();

        issueRequest = new IssueRequest();
        issueRequest.setTitle("Broken elevator");
        issueRequest.setDescription("Elevator stuck on 3rd floor");
    }

    // ── createIssue tests ──

    @Test
    void createIssue_success_returnsResponse() {
        when(issueRepository.save(any(Issue.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(any())).thenReturn(Optional.of(creator));
        when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

        IssueResponse response = issueService.createIssue(issueRequest, "creator123");

        assertThat(response.getTitle()).isEqualTo("Broken elevator");
        assertThat(response.getDescription()).isEqualTo("Elevator stuck on 3rd floor");
        assertThat(response.getStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(response.getPriority()).isEqualTo(IssuePriority.LOW);
    }

    @Test
    void createIssue_defaultsToOpenStatus() {
        when(issueRepository.save(any(Issue.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(any())).thenReturn(Optional.of(creator));
        when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

        IssueResponse response = issueService.createIssue(issueRequest, "creator123");

        assertThat(response.getStatus()).isEqualTo(IssueStatus.OPEN);
    }

    @Test
    void createIssue_defaultsToLowPriority() {
        when(issueRepository.save(any(Issue.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(any())).thenReturn(Optional.of(creator));
        when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

        IssueResponse response = issueService.createIssue(issueRequest, "creator123");

        assertThat(response.getPriority()).isEqualTo(IssuePriority.LOW);
    }

    // ── getIssues tests ──

    @Test
    void getIssues_noFilter_returnsAll() {
        Page<Issue> page = new PageImpl<>(List.of(issue));
        when(issueRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(userRepository.findById(any())).thenReturn(Optional.of(creator));
        when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

        PagedResponse<IssueResponse> result =
                issueService.getIssues(null, "createdAt", "desc", 0, 20);

        assertThat(result.getContent()).hasSize(1);
        verify(issueRepository).findAll(any(Pageable.class));
    }

    @Test
    void getIssues_filterByStatus_callsCorrectRepository() {
        Page<Issue> page = new PageImpl<>(List.of(issue));
        when(issueRepository.findByStatus(eq(IssueStatus.OPEN), any(Pageable.class)))
                .thenReturn(page);
        when(userRepository.findById(any())).thenReturn(Optional.of(creator));
        when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

        issueService.getIssues(IssueStatus.OPEN, "createdAt", "desc", 0, 20);

        verify(issueRepository).findByStatus(eq(IssueStatus.OPEN), any(Pageable.class));
        verify(issueRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getIssues_sortByVoteCount_sortsDescending() {
        Issue issue1 = Issue.builder().title("Issue 1").creatorId("c1").build();
        Issue issue2 = Issue.builder().title("Issue 2").creatorId("c2").build();

        Page<Issue> page = new PageImpl<>(List.of(issue1, issue2));
        when(issueRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(userRepository.findById(any())).thenReturn(Optional.of(creator));
        when(issueVoteRepository.countByIssueId(null)).thenReturn(5L, 2L);

        PagedResponse<IssueResponse> result =
                issueService.getIssues(null, "voteCount", "desc", 0, 20);

        assertThat(result.getContent().get(0).getVoteCount())
                .isGreaterThanOrEqualTo(result.getContent().get(1).getVoteCount());
    }

    // ── updateStatus tests ──

    @Test
    void updateStatus_success_returnsUpdatedIssue() {
        when(issueRepository.findById("issue1")).thenReturn(Optional.of(issue));
        when(issueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(any())).thenReturn(Optional.of(creator));
        when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

        IssueResponse response = issueService.updateStatus("issue1", IssueStatus.IN_PROGRESS);

        assertThat(response.getStatus()).isEqualTo(IssueStatus.IN_PROGRESS);
    }

    @Test
    void updateStatus_issueNotFound_throwsNotFound() {
        when(issueRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                issueService.updateStatus("nonexistent", IssueStatus.RESOLVED))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Issue not found");
    }

    // ── updatePriority tests ──

    @Test
    void updatePriority_success_returnsUpdatedIssue() {
        when(issueRepository.findById("issue1")).thenReturn(Optional.of(issue));
        when(issueRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findById(any())).thenReturn(Optional.of(creator));
        when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

        IssueResponse response = issueService.updatePriority("issue1", IssuePriority.HIGH);

        assertThat(response.getPriority()).isEqualTo(IssuePriority.HIGH);
    }

    @Test
    void updatePriority_issueNotFound_throwsNotFound() {
        when(issueRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                issueService.updatePriority("nonexistent", IssuePriority.HIGH))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Issue not found");
    }

    // ── addVote tests ──

    @Test
    void addVote_success_savesVote() {
        issue.setCreatorId("creator123");
        when(issueRepository.findById("issue1")).thenReturn(Optional.of(issue));
        when(issueVoteRepository.existsByIssueIdAndUserId("issue1", "voter456"))
                .thenReturn(false);

        issueService.addVote("issue1", "voter456");

        verify(issueVoteRepository).save(argThat(vote ->
                vote.getIssueId().equals("issue1") &&
                        vote.getUserId().equals("voter456")
        ));
    }

    @Test
    void addVote_ownIssue_throwsBadRequest() {
        issue.setCreatorId("creator123");
        when(issueRepository.findById("issue1")).thenReturn(Optional.of(issue));

        assertThatThrownBy(() -> issueService.addVote("issue1", "creator123"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot vote on your own issue");

        verify(issueVoteRepository, never()).save(any());
    }

    @Test
    void addVote_alreadyVoted_throwsConflict() {
        issue.setCreatorId("creator123");
        when(issueRepository.findById("issue1")).thenReturn(Optional.of(issue));
        when(issueVoteRepository.existsByIssueIdAndUserId("issue1", "voter456"))
                .thenReturn(true);

        assertThatThrownBy(() -> issueService.addVote("issue1", "voter456"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Already voted on this issue");

        verify(issueVoteRepository, never()).save(any());
    }

    @Test
    void addVote_issueNotFound_throwsNotFound() {
        when(issueRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.addVote("nonexistent", "voter456"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Issue not found");
    }

    // ── removeVote tests ──

    @Test
    void removeVote_success_deletesVote() {
        IssueVote vote = IssueVote.builder()
                .issueId("issue1")
                .userId("voter456")
                .build();
        when(issueVoteRepository.findByIssueIdAndUserId("issue1", "voter456"))
                .thenReturn(Optional.of(vote));

        issueService.removeVote("issue1", "voter456");

        verify(issueVoteRepository).delete(vote);
    }

    @Test
    void removeVote_voteNotFound_throwsNotFound() {
        when(issueVoteRepository.findByIssueIdAndUserId("issue1", "voter456"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.removeVote("issue1", "voter456"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vote not found");
    }
}