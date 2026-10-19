package com.Application.SocietyManagement.users.service;

import com.Application.SocietyManagement.communication.service.SocietyGroupService;
import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.dto.UpdateResidentRequest;
import com.Application.SocietyManagement.users.dto.UserResponseDto;
import com.Application.SocietyManagement.users.dto.UserSummarydto;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.PlatformRole;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final SocietyGroupService societyGroupService;
    private final PasswordEncoder passwordEncoder;

    public UserSummarydto updateStatus(String userId, Status status) {
        String societyId = requireSocietyId();
        User user = userRepository.findByIdAndSocietyId(userId, societyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        if (user.getRole() == Roles.ADMIN ||
                user.getRole() == Roles.SUPER_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Cannot modify another admin");
        }

        user.setStatus(status);
        User saved = userRepository.save(user);

        // Auto-join society community group upon activation
        if (status == Status.ACTIVE && saved.getSocietyId() != null && societyGroupService != null) {
            try {
                societyGroupService.autoJoinResident(saved.getSocietyId(), saved.getId());
            } catch (Exception e) {
                log.warn("Could not auto-join user {} to society group: {}", saved.getId(), e.getMessage());
            }
        }

        return UserSummarydto.from(saved);
    }

    public PagedResponse<UserSummarydto> getUsers(int page, int size,
                                                  Status status, Roles role) {
        String societyId = requireSocietyId();
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> result;

        if (status != null && role != null) {
            result = userRepository.findBySocietyIdAndStatusAndRole(societyId, status, role, pageable);
        } else if (status != null) {
            result = userRepository.findBySocietyIdAndStatus(societyId, status, pageable);
        } else if (role != null) {
            result = userRepository.findBySocietyIdAndRole(societyId, role, pageable);
        } else {
            result = userRepository.findBySocietyId(societyId, pageable);
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
        String societyId = requireSocietyId();
        return userRepository.findBySocietyId(societyId, Pageable.unpaged())
                .getContent()
                .stream()
                .map(UserResponseDto::from)
                .toList();
    }

    private String requireSocietyId() {
        String societyId = TenantContext.getSocietyId();
        if (societyId == null || societyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No society context");
        }
        return societyId;
    }

    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + email));
        return UserResponseDto.from(user);
    }

    public UserResponseDto getCurrentUser(User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        User user = userRepository.findById(currentUser.getId()).orElse(currentUser);
        return UserResponseDto.from(user);
    }

    public void changePassword(User currentUser, com.Application.SocietyManagement.users.dto.ChangePasswordRequest request, org.springframework.security.crypto.password.PasswordEncoder encoder) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        org.springframework.security.crypto.password.PasswordEncoder effectiveEncoder = (encoder != null) ? encoder : this.passwordEncoder;
        if (effectiveEncoder != null) {
            if (!effectiveEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password does not match");
            }
            user.setPasswordHash(effectiveEncoder.encode(request.getNewPassword()));
        }
        userRepository.save(user);
    }

    /**
     * Deletes a user permanently from the platform.
     * STRICT GOVERNANCE RULE: Restricted exclusively to PLATFORM_ADMIN.
     * Society admins and residents CANNOT delete any user from the platform or group.
     */
    public void deleteUser(String userId, User currentUser) {
        if (currentUser == null || currentUser.getPlatformRole() != PlatformRole.PLATFORM_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied. Only platform administrators can delete users from the platform."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));

        if (currentUser.getId() != null && currentUser.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete your own platform admin account");
        }

        // Clean up user from all society groups
        if (societyGroupService != null) {
            societyGroupService.removeUserFromAllGroups(userId);
        }

        userRepository.delete(user);
        log.info("Platform admin {} permanently deleted user {}", currentUser.getEmail(), userId);
    }

    /**
     * Updates an existing resident's details (e.g. resident replacement, tenancy handover).
     * Society admins can update/replace a resident with new resident details, but CANNOT delete the user.
     */
    public UserResponseDto updateResident(String userId, UpdateResidentRequest request, User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        User user;
        if (currentUser.getPlatformRole() == PlatformRole.PLATFORM_ADMIN) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userId));
        } else {
            String societyId = requireSocietyId();
            user = userRepository.findByIdAndSocietyId(userId, societyId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resident not found in this society"));

            if (user.getPlatformRole() == PlatformRole.PLATFORM_ADMIN) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify platform administrator");
            }
        }

        // Check email uniqueness if email is modified
        if (request.getEmail() != null && !request.getEmail().trim().equalsIgnoreCase(user.getEmail())) {
            String newEmail = request.getEmail().trim();
            if (userRepository.findByEmail(newEmail).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered to another user");
            }
            user.setEmail(newEmail);
        }

        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            user.setPhone(request.getPhone().trim());
        }
        if (request.getFlatId() != null) {
            user.setFlatId(request.getFlatId().trim());
        }
        if (request.getIsOwner() != null) {
            user.setOwner(request.getIsOwner());
        }
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank() && passwordEncoder != null) {
            user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        }

        User saved = userRepository.save(user);

        // Ensure user remains auto-joined to their society group
        if (saved.getSocietyId() != null && societyGroupService != null) {
            try {
                societyGroupService.autoJoinResident(saved.getSocietyId(), saved.getId());
            } catch (Exception e) {
                log.debug("Auto-join check on update resident: {}", e.getMessage());
            }
        }

        log.info("Resident {} updated/replaced by {}", saved.getId(), currentUser.getEmail());
        return UserResponseDto.from(saved);
    }
}
