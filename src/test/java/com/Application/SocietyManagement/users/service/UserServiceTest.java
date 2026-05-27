package com.Application.SocietyManagement.users.service;

import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.dto.UserSummarydto;
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
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User residentUser;
    private User adminUser;
    private User accountantUser;
    private User securityUser;

    @BeforeEach
    void setUp() {
        TenantContext.setSocietyId("test-society-id");

        residentUser = User.builder()
                .email("resident@test.com")
                .firstName("John")
                .lastName("Doe")
                .apartmentNumber("A-101")
                .role(Roles.RESIDENT)
                .status(Status.PENDING)
                .societyId("test-society-id")
                .build();

        adminUser = User.builder()
                .email("admin@test.com")
                .firstName("Admin")
                .lastName("User")
                .role(Roles.ADMIN)
                .status(Status.ACTIVE)
                .societyId("test-society-id")
                .build();

        accountantUser = User.builder()
                .email("accountant@test.com")
                .firstName("Account")
                .lastName("User")
                .role(Roles.ACCOUNTANT)
                .status(Status.ACTIVE)
                .societyId("test-society-id")
                .build();

        securityUser = User.builder()
                .email("security@test.com")
                .firstName("Security")
                .lastName("Guard")
                .role(Roles.SECURITY)
                .status(Status.ACTIVE)
                .societyId("test-society-id")
                .build();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("PENDING to ACTIVE - approves resident successfully")
        void updateStatus_pendingToActive_approvesResident() {
            when(userRepository.findById("user1"))
                    .thenReturn(Optional.of(residentUser));

            when(userRepository.save(any(User.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            UserSummarydto result =
                    userService.updateStatus("user1", Status.ACTIVE);

            assertThat(result.getStatus()).isEqualTo(Status.ACTIVE);

            verify(userRepository).save(argThat(u ->
                    u.getStatus() == Status.ACTIVE));
        }

        @Test
        @DisplayName("ACTIVE to INACTIVE - deactivates resident successfully")
        void updateStatus_activeToInactive_deactivatesResident() {
            residentUser.setStatus(Status.ACTIVE);

            when(userRepository.findById("user1"))
                    .thenReturn(Optional.of(residentUser));

            when(userRepository.save(any(User.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            UserSummarydto result =
                    userService.updateStatus("user1", Status.INACTIVE);

            assertThat(result.getStatus()).isEqualTo(Status.INACTIVE);
        }

        @Test
        @DisplayName("ACTIVE to BLOCKED - blocks resident")
        void updateStatus_activeToBlocked_blocksResident() {
            residentUser.setStatus(Status.ACTIVE);

            when(userRepository.findById("user1"))
                    .thenReturn(Optional.of(residentUser));

            when(userRepository.save(any(User.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            UserSummarydto result =
                    userService.updateStatus("user1", Status.BLOCKED);

            assertThat(result.getStatus()).isEqualTo(Status.BLOCKED);
        }

        @Test
        @DisplayName("user not found - throws 404 NOT FOUND")
        void updateStatus_userNotFound_throwsNotFound() {
            when(userRepository.findById("nonexistent"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    userService.updateStatus("nonexistent", Status.ACTIVE))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e =
                                (ResponseStatusException) ex;

                        assertThat(e.getStatusCode().value())
                                .isEqualTo(404);

                        assertThat(e.getMessage())
                                .contains("User not found");
                    });
        }

        @Test
        @DisplayName("modifying ADMIN - throws 403 FORBIDDEN")
        void updateStatus_adminUser_throwsForbidden() {
            when(userRepository.findById("adminId"))
                    .thenReturn(Optional.of(adminUser));

            assertThatThrownBy(() ->
                    userService.updateStatus("adminId", Status.INACTIVE))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e =
                                (ResponseStatusException) ex;

                        assertThat(e.getStatusCode().value())
                                .isEqualTo(403);

                        assertThat(e.getMessage())
                                .contains("Cannot modify another admin");
                    });

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("modifying SUPER_ADMIN - throws 403 FORBIDDEN")
        void updateStatus_superAdminUser_throwsForbidden() {

            User superAdmin = User.builder()
                    .email("superadmin@test.com")
                    .firstName("Super")
                    .lastName("Admin")
                    .role(Roles.SUPER_ADMIN)
                    .status(Status.ACTIVE)
                    .societyId("test-society-id")
                    .build();

            when(userRepository.findById("superAdminId"))
                    .thenReturn(Optional.of(superAdmin));

            assertThatThrownBy(() ->
                    userService.updateStatus(
                            "superAdminId",
                            Status.INACTIVE))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e =
                                (ResponseStatusException) ex;

                        assertThat(e.getStatusCode().value())
                                .isEqualTo(403);

                        assertThat(e.getMessage())
                                .contains("Cannot modify another admin");
                    });

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUsers")
    class GetUsers {

        @Test
        @DisplayName("no filters - returns all users")
        void getUsers_noFilters_returnsAllUsers() {

            Page<User> page =
                    new PageImpl<>(List.of(residentUser, adminUser));

            when(userRepository.findAll(any(Pageable.class)))
                    .thenReturn(page);

            PagedResponse<UserSummarydto> result =
                    userService.getUsers(0, 20, null, null);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("filter by PENDING status - calls findByStatus")
        void getUsers_filterByStatus_callsCorrectRepository() {

            Page<User> page =
                    new PageImpl<>(List.of(residentUser));

            when(userRepository.findByStatus(
                    eq(Status.PENDING),
                    any(Pageable.class)))
                    .thenReturn(page);

            PagedResponse<UserSummarydto> result =
                    userService.getUsers(
                            0,
                            20,
                            Status.PENDING,
                            null);

            assertThat(result.getContent()).hasSize(1);

            assertThat(result.getContent()
                    .getFirst()
                    .getStatus())
                    .isEqualTo(Status.PENDING);

            verify(userRepository).findByStatus(
                    eq(Status.PENDING),
                    any(Pageable.class));

            verify(userRepository, never())
                    .findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("filter by RESIDENT role - calls findByRole")
        void getUsers_filterByRole_callsCorrectRepository() {

            Page<User> page =
                    new PageImpl<>(List.of(residentUser));

            when(userRepository.findByRole(
                    eq(Roles.RESIDENT),
                    any(Pageable.class)))
                    .thenReturn(page);

            PagedResponse<UserSummarydto> result =
                    userService.getUsers(
                            0,
                            20,
                            null,
                            Roles.RESIDENT);

            assertThat(result.getContent()).hasSize(1);

            verify(userRepository).findByRole(
                    eq(Roles.RESIDENT),
                    any(Pageable.class));
        }

        @Test
        @DisplayName("filter by ACCOUNTANT role - returns accountants")
        void getUsers_filterByAccountantRole_returnsAccountants() {

            Page<User> page =
                    new PageImpl<>(List.of(accountantUser));

            when(userRepository.findByRole(
                    eq(Roles.ACCOUNTANT),
                    any(Pageable.class)))
                    .thenReturn(page);

            PagedResponse<UserSummarydto> result =
                    userService.getUsers(
                            0,
                            20,
                            null,
                            Roles.ACCOUNTANT);

            assertThat(result.getContent()).hasSize(1);

            verify(userRepository).findByRole(
                    eq(Roles.ACCOUNTANT),
                    any(Pageable.class));
        }

        @Test
        @DisplayName("filter by SECURITY role - returns security guards")
        void getUsers_filterBySecurityRole_returnsSecurityGuards() {

            Page<User> page =
                    new PageImpl<>(List.of(securityUser));

            when(userRepository.findByRole(
                    eq(Roles.SECURITY),
                    any(Pageable.class)))
                    .thenReturn(page);

            PagedResponse<UserSummarydto> result =
                    userService.getUsers(
                            0,
                            20,
                            null,
                            Roles.SECURITY);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("filter by status AND role - calls findByStatusAndRole")
        void getUsers_filterByStatusAndRole_callsCorrectRepository() {

            Page<User> page =
                    new PageImpl<>(List.of(residentUser));

            when(userRepository.findByStatusAndRole(
                    eq(Status.PENDING),
                    eq(Roles.RESIDENT),
                    any(Pageable.class)))
                    .thenReturn(page);

            PagedResponse<UserSummarydto> result =
                    userService.getUsers(
                            0,
                            20,
                            Status.PENDING,
                            Roles.RESIDENT);

            assertThat(result.getContent()).hasSize(1);

            verify(userRepository).findByStatusAndRole(
                    eq(Status.PENDING),
                    eq(Roles.RESIDENT),
                    any(Pageable.class));
        }

        @Test
        @DisplayName("empty result - returns empty page")
        void getUsers_emptyResult_returnsEmptyPage() {

            Page<User> emptyPage =
                    new PageImpl<>(List.of());

            when(userRepository.findAll(any(Pageable.class)))
                    .thenReturn(emptyPage);

            PagedResponse<UserSummarydto> result =
                    userService.getUsers(0, 20, null, null);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("pagination metadata - correct page info")
        void getUsers_paginationMetadata_isCorrect() {

            Page<User> page = new PageImpl<>(
                    List.of(residentUser),
                    PageRequest.of(
                            0,
                            20,
                            Sort.by(
                                    Sort.Direction.DESC,
                                    "createdAt")),
                    50
            );

            when(userRepository.findAll(any(Pageable.class)))
                    .thenReturn(page);

            PagedResponse<UserSummarydto> result =
                    userService.getUsers(0, 20, null, null);

            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(20);
            assertThat(result.getTotalElements()).isEqualTo(50);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }

        @Test
        @DisplayName("password hash never exposed in response")
        void getUsers_passwordHashNeverExposedInResponse() {

            Page<User> page =
                    new PageImpl<>(List.of(residentUser));

            when(userRepository.findAll(any(Pageable.class)))
                    .thenReturn(page);

            PagedResponse<UserSummarydto> result =
                    userService.getUsers(0, 20, null, null);

            result.getContent().forEach(dto ->
                    assertThat(dto)
                            .doesNotHaveToString("password"));
        }
    }
}