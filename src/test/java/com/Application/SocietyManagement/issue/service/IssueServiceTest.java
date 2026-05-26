package com.Application.SocietyManagement.issue.service;

import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.issue.dto.IssueRequest;
import com.Application.SocietyManagement.issue.dto.IssueResponse;
import com.Application.SocietyManagement.issue.entity.Issue;
import com.Application.SocietyManagement.issue.entity.IssueVote;
import com.Application.SocietyManagement.issue.enums.IssuePriority;
import com.Application.SocietyManagement.issue.enums.IssueStatus;
import com.Application.SocietyManagement.issue.repository.IssueRepository;
import com.Application.SocietyManagement.issue.repository.IssueVoteRepository;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import org.junit.jupiter.api.*;
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
@DisplayName("IssueService")
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
        TenantContext.setSocietyId("test-society-id");

        creator = User.builder()
                .email("resident@test.com")
                .firstName("John")
                .lastName("Doe")
                .role(Roles.RESIDENT)
                .status(Status.ACTIVE)
                .societyId("test-society-id")
                .build();

        issue = Issue.builder()
                .title("Broken elevator")
                .description("Elevator stuck on 3rd floor")
                .creatorId("creator123")
                .societyId("test-society-id")
                .build();

        issueRequest = new IssueRequest();
        issueRequest.setTitle("Broken elevator");
        issueRequest.setDescription("Elevator stuck on 3rd floor");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("createIssue")
    class CreateIssue {

        @Test
        @DisplayName("success - returns correct response")
        void createIssue_success_returnsResponse() {
            when(issueRepository.save(any(Issue.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(any()))
                    .thenReturn(Optional.of(creator));
            when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

            IssueResponse response =
                    issueService.createIssue(issueRequest, "creator123");

            assertThat(response.getTitle()).isEqualTo("Broken elevator");
            assertThat(response.getDescription())
                    .isEqualTo("Elevator stuck on 3rd floor");
            assertThat(response.getStatus()).isEqualTo(IssueStatus.OPEN);
            assertThat(response.getPriority()).isEqualTo(IssuePriority.LOW);
        }

        @Test
        @DisplayName("default status is OPEN")
        void createIssue_defaultsToOpenStatus() {
            when(issueRepository.save(any(Issue.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(any()))
                    .thenReturn(Optional.of(creator));
            when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

            IssueResponse response =
                    issueService.createIssue(issueRequest, "creator123");

            assertThat(response.getStatus()).isEqualTo(IssueStatus.OPEN);
        }

        @Test
        @DisplayName("default priority is LOW")
        void createIssue_defaultsToLowPriority() {
            when(issueRepository.save(any(Issue.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(any()))
                    .thenReturn(Optional.of(creator));
            when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

            IssueResponse response =
                    issueService.createIssue(issueRequest, "creator123");

            assertThat(response.getPriority()).isEqualTo(IssuePriority.LOW);
        }

        @Test
        @DisplayName("societyId set from TenantContext")
        void createIssue_societyIdSetFromTenantContext() {
            when(issueRepository.save(any(Issue.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(any()))
                    .thenReturn(Optional.of(creator));
            when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

            issueService.createIssue(issueRequest, "creator123");

            verify(issueRepository).save(argThat(i ->
                    "test-society-id".equals(i.getSocietyId())));
        }

        @Test
        @DisplayName("initial vote count is zero")
        void createIssue_initialVoteCount_isZero() {
            when(issueRepository.save(any(Issue.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(any()))
                    .thenReturn(Optional.of(creator));
            when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

            IssueResponse response =
                    issueService.createIssue(issueRequest, "creator123");

            assertThat(response.getVoteCount()).isZero();
        }
    }

    @Nested
    @DisplayName("getIssues")
    class GetIssues {

        @Test
        @DisplayName("no filter - returns all for society")
        void getIssues_noFilter_returnsAll() {
            Page<Issue> page = new PageImpl<>(List.of(issue));
            when(issueRepository.findBySocietyId(
                    eq("test-society-id"), any(Pageable.class)))
                    .thenReturn(page);
            when(userRepository.findAllById(any()))
                    .thenReturn(List.of(creator));
            when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

            PagedResponse<IssueResponse> result =
                    issueService.getIssues(null, "createdAt", "desc", 0, 20);

            assertThat(result.getContent()).hasSize(1);
            verify(issueRepository).findBySocietyId(
                    eq("test-society-id"), any(Pageable.class));
        }

        @Test
        @DisplayName("filter by OPEN status - calls correct repository")
        void getIssues_filterByStatus_callsCorrectRepository() {
            Page<Issue> page = new PageImpl<>(List.of(issue));
            when(issueRepository.findByStatusAndSocietyId(
                    eq(IssueStatus.OPEN),
                    eq("test-society-id"),
                    any(Pageable.class)))
                    .thenReturn(page);
            when(userRepository.findAllById(any()))
                    .thenReturn(List.of(creator));
            when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

            issueService.getIssues(IssueStatus.OPEN, "createdAt", "desc", 0, 20);

            verify(issueRepository).findByStatusAndSocietyId(
                    eq(IssueStatus.OPEN),
                    eq("test-society-id"),
                    any(Pageable.class));
            verify(issueRepository, never())
                    .findBySocietyId(any(), any(Pageable.class));
        }

        @Test
        @DisplayName("sort by voteCount descending - sorts correctly")
        void getIssues_sortByVoteCount_sortsDescending() {
            Issue issue1 = Issue.builder()
                    .title("Issue 1").creatorId("c1")
                    .societyId("test-society-id").build();
            Issue issue2 = Issue.builder()
                    .title("Issue 2").creatorId("c2")
                    .societyId("test-society-id").build();

            Page<Issue> page = new PageImpl<>(List.of(issue1, issue2));
            when(issueRepository.findBySocietyId(
                    eq("test-society-id"), any(Pageable.class)))
                    .thenReturn(page);
            when(userRepository.findAllById(any()))
                    .thenReturn(List.of(creator));
            when(issueVoteRepository.countByIssueId(null))
                    .thenReturn(5L, 2L);

            PagedResponse<IssueResponse> result =
                    issueService.getIssues(null, "voteCount", "desc", 0, 20);

            assertThat(result.getContent().get(0).getVoteCount())
                    .isGreaterThanOrEqualTo(
                            result.getContent().get(1).getVoteCount());
        }

        @Test
        @DisplayName("batch fetch creators - no N+1 queries")
        void getIssues_batchFetchesCreators_noNPlusOne() {
            Issue issue1 = Issue.builder()
                    .title("Issue 1").creatorId("c1")
                    .societyId("test-society-id").build();
            Issue issue2 = Issue.builder()
                    .title("Issue 2").creatorId("c2")
                    .societyId("test-society-id").build();

            Page<Issue> page = new PageImpl<>(List.of(issue1, issue2));
            when(issueRepository.findBySocietyId(
                    eq("test-society-id"), any(Pageable.class)))
                    .thenReturn(page);
            when(userRepository.findAllById(any()))
                    .thenReturn(List.of(creator));
            when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

            issueService.getIssues(null, "createdAt", "desc", 0, 20);

            // verify single batch call not per-issue call
            verify(userRepository, times(1)).findAllById(any());
            verify(userRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("OPEN to IN_PROGRESS - updates successfully")
        void updateStatus_openToInProgress_updatesSuccessfully() {
            when(issueRepository.findById("issue1"))
                    .thenReturn(Optional.of(issue));
            when(issueRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(any()))
                    .thenReturn(Optional.of(creator));
            when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

            IssueResponse response =
                    issueService.updateStatus("issue1", IssueStatus.IN_PROGRESS);

            assertThat(response.getStatus()).isEqualTo(IssueStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("OPEN to RESOLVED - updates successfully")
        void updateStatus_openToResolved_updatesSuccessfully() {
            when(issueRepository.findById("issue1"))
                    .thenReturn(Optional.of(issue));
            when(issueRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(any()))
                    .thenReturn(Optional.of(creator));
            when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

            IssueResponse response =
                    issueService.updateStatus("issue1", IssueStatus.RESOLVED);

            assertThat(response.getStatus()).isEqualTo(IssueStatus.RESOLVED);
        }

        @Test
        @DisplayName("not found - throws 404")
        void updateStatus_issueNotFound_throwsNotFound() {
            when(issueRepository.findById("nonexistent"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    issueService.updateStatus("nonexistent",
                            IssueStatus.RESOLVED))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(404);
                    });
        }
    }

    @Nested
    @DisplayName("updatePriority")
    class UpdatePriority {

        @Test
        @DisplayName("LOW to HIGH - updates successfully")
        void updatePriority_lowToHigh_updatesSuccessfully() {
            when(issueRepository.findById("issue1"))
                    .thenReturn(Optional.of(issue));
            when(issueRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findById(any()))
                    .thenReturn(Optional.of(creator));
            when(issueVoteRepository.countByIssueId(any())).thenReturn(0L);

            IssueResponse response =
                    issueService.updatePriority("issue1", IssuePriority.HIGH);

            assertThat(response.getPriority()).isEqualTo(IssuePriority.HIGH);
        }

        @Test
        @DisplayName("not found - throws 404")
        void updatePriority_issueNotFound_throwsNotFound() {
            when(issueRepository.findById("nonexistent"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    issueService.updatePriority("nonexistent",
                            IssuePriority.HIGH))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(404);
                    });
        }
    }

    @Nested
    @DisplayName("addVote")
    class AddVote {

        @Test
        @DisplayName("success - saves vote")
        void addVote_success_savesVote() {
            issue.setCreatorId("creator123");
            when(issueRepository.findById("issue1"))
                    .thenReturn(Optional.of(issue));
            when(issueVoteRepository.existsByIssueIdAndUserId(
                    "issue1", "voter456"))
                    .thenReturn(false);

            issueService.addVote("issue1", "voter456");

            verify(issueVoteRepository).save(argThat(vote ->
                    vote.getIssueId().equals("issue1") &&
                            vote.getUserId().equals("voter456")));
        }

        @Test
        @DisplayName("own issue - throws 400 BAD REQUEST")
        void addVote_ownIssue_throwsBadRequest() {
            issue.setCreatorId("creator123");
            when(issueRepository.findById("issue1"))
                    .thenReturn(Optional.of(issue));

            assertThatThrownBy(() ->
                    issueService.addVote("issue1", "creator123"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(400);
                        assertThat(e.getMessage())
                                .contains("Cannot vote on your own issue");
                    });

            verify(issueVoteRepository, never()).save(any());
        }

        @Test
        @DisplayName("already voted - throws 409 CONFLICT")
        void addVote_alreadyVoted_throwsConflict() {
            issue.setCreatorId("creator123");
            when(issueRepository.findById("issue1"))
                    .thenReturn(Optional.of(issue));
            when(issueVoteRepository.existsByIssueIdAndUserId(
                    "issue1", "voter456"))
                    .thenReturn(true);

            assertThatThrownBy(() ->
                    issueService.addVote("issue1", "voter456"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(409);
                        assertThat(e.getMessage())
                                .contains("Already voted on this issue");
                    });

            verify(issueVoteRepository, never()).save(any());
        }

        @Test
        @DisplayName("issue not found - throws 404")
        void addVote_issueNotFound_throwsNotFound() {
            when(issueRepository.findById("nonexistent"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    issueService.addVote("nonexistent", "voter456"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(404);
                    });
        }
    }

    @Nested
    @DisplayName("removeVote")
    class RemoveVote {

        @Test
        @DisplayName("success - deletes vote")
        void removeVote_success_deletesVote() {
            IssueVote vote = IssueVote.builder()
                    .issueId("issue1")
                    .userId("voter456")
                    .build();
            when(issueVoteRepository.findByIssueIdAndUserId(
                    "issue1", "voter456"))
                    .thenReturn(Optional.of(vote));

            issueService.removeVote("issue1", "voter456");

            verify(issueVoteRepository).delete(vote);
        }

        @Test
        @DisplayName("vote not found - throws 404")
        void removeVote_voteNotFound_throwsNotFound() {
            when(issueVoteRepository.findByIssueIdAndUserId(
                    "issue1", "voter456"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    issueService.removeVote("issue1", "voter456"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(404);
                        assertThat(e.getMessage()).contains("Vote not found");
                    });
        }
    }
}