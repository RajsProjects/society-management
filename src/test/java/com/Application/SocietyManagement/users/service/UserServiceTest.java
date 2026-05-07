package com.Application.SocietyManagement.users.service;

import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.dto.UserSummarydto;
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
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User residentUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        residentUser = User.builder()
                .email("resident@test.com")
                .firstName("John")
                .lastName("Doe")
                .apartmentNumber("A-101")
                .role(Roles.RESIDENT)
                .status(Status.PENDING)
                .build();

        adminUser = User.builder()
                .email("admin@test.com")
                .firstName("Admin")
                .lastName("User")
                .role(Roles.ADMIN)
                .status(Status.ACTIVE)
                .build();
    }

    // ── updateStatus tests ──

    @Test
    void updateStatus_success_returnsUpdatedUser() {
        when(userRepository.findById("user1"))
                .thenReturn(Optional.of(residentUser));
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserSummarydto result = userService.updateStatus("user1", Status.ACTIVE);

        assertThat(result.getStatus()).isEqualTo(Status.ACTIVE);
        verify(userRepository).save(argThat(u -> u.getStatus() == Status.ACTIVE));
    }

    @Test
    void updateStatus_userNotFound_throwsNotFound() {
        when(userRepository.findById("nonexistent"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateStatus("nonexistent", Status.ACTIVE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateStatus_adminUser_throwsForbidden() {
        when(userRepository.findById("adminId"))
                .thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> userService.updateStatus("adminId", Status.INACTIVE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot modify another admin");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateStatus_toInactive_persistsCorrectly() {
        when(userRepository.findById("user1"))
                .thenReturn(Optional.of(residentUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserSummarydto result = userService.updateStatus("user1", Status.INACTIVE);

        assertThat(result.getStatus()).isEqualTo(Status.INACTIVE);
    }

    // ── getUsers tests ──

    @Test
    void getUsers_noFilters_returnsAllUsers() {
        Page<User> page = new PageImpl<>(List.of(residentUser, adminUser));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        PagedResponse<UserSummarydto> result =
                userService.getUsers(0, 20, null, null);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void getUsers_filterByStatus_callsCorrectRepository() {
        Page<User> page = new PageImpl<>(List.of(residentUser));
        when(userRepository.findByStatus(eq(Status.PENDING), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<UserSummarydto> result =
                userService.getUsers(0, 20, Status.PENDING, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(Status.PENDING);
        verify(userRepository).findByStatus(eq(Status.PENDING), any(Pageable.class));
        verify(userRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void getUsers_filterByRole_callsCorrectRepository() {
        Page<User> page = new PageImpl<>(List.of(residentUser));
        when(userRepository.findByRole(eq(Roles.RESIDENT), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<UserSummarydto> result =
                userService.getUsers(0, 20, null, Roles.RESIDENT);

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByRole(eq(Roles.RESIDENT), any(Pageable.class));
    }

    @Test
    void getUsers_filterByStatusAndRole_callsCorrectRepository() {
        Page<User> page = new PageImpl<>(List.of(residentUser));
        when(userRepository.findByStatusAndRole(
                eq(Status.PENDING), eq(Roles.RESIDENT), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<UserSummarydto> result =
                userService.getUsers(0, 20, Status.PENDING, Roles.RESIDENT);

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByStatusAndRole(
                eq(Status.PENDING), eq(Roles.RESIDENT), any(Pageable.class));
    }

    @Test
    void getUsers_emptyResult_returnsEmptyPage() {
        Page<User> emptyPage = new PageImpl<>(List.of());
        when(userRepository.findAll(any(Pageable.class))).thenReturn(emptyPage);

        PagedResponse<UserSummarydto> result =
                userService.getUsers(0, 20, null, null);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getUsers_paginationMetadata_isCorrect() {
        Page<User> page = new PageImpl<>(
                List.of(residentUser),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                50
        );
        when(userRepository.findAll(any(Pageable.class))).thenReturn(page);

        PagedResponse<UserSummarydto> result =
                userService.getUsers(0, 20, null, null);

        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(20);
        assertThat(result.getTotalElements()).isEqualTo(50);
        assertThat(result.getTotalPages()).isEqualTo(3);
    }
}