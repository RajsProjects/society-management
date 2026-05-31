package com.Application.SocietyManagement.users.service;

import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.dto.UserResponseDto;
import com.Application.SocietyManagement.users.dto.UserSummarydto;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserSummarydto updateStatus(String userId, Status status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == Roles.ADMIN ||
                user.getRole() == Roles.SUPER_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Cannot modify another admin");
        }

        user.setStatus(status);
        return UserSummarydto.from(userRepository.save(user));
    }

    public PagedResponse<UserSummarydto> getUsers(int page, int size,
                                                  Status status, Roles role) {
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> result;

        if (status != null && role != null) {
            result = userRepository.findByStatusAndRole(status, role, pageable);
        } else if (status != null) {
            result = userRepository.findByStatus(status, pageable);
        } else if (role != null) {
            result = userRepository.findByRole(role, pageable);
        } else {
            result = userRepository.findAll(pageable);
        }

        return PagedResponse.<UserSummarydto>builder()
                .content(result.getContent().stream()
                        .map(UserSummarydto::from)
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    public List<UserResponseDto> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserResponseDto::from)
                .toList();
    }

    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return UserResponseDto.from(user);
    }
    
}
