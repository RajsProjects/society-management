package com.Application.SocietyManagement.communication.service;

import com.Application.SocietyManagement.communication.dto.GroupMemberDto;
import com.Application.SocietyManagement.communication.dto.SocietyGroupResponse;
import com.Application.SocietyManagement.communication.entity.SocietyGroup;
import com.Application.SocietyManagement.communication.repository.SocietyGroupRepository;
import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.flat.dto.FlatResponse;
import com.Application.SocietyManagement.flat.entity.Flat;
import com.Application.SocietyManagement.flat.repository.FlatRepository;
import com.Application.SocietyManagement.society.entity.Society;
import com.Application.SocietyManagement.society.repository.SocietyRepository;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.PlatformRole;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocietyGroupService {

    private final SocietyGroupRepository societyGroupRepository;
    private final SocietyRepository societyRepository;
    private final UserRepository userRepository;
    private final FlatRepository flatRepository;

    /**
     * Creates or fetches the single official group for the given society.
     * Exactly 1 group per society.
     */
    public SocietyGroup createOrGetGroupForSociety(Society society) {
        if (society == null || society.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid society reference");
        }

        return societyGroupRepository.findBySocietyId(society.getId())
                .orElseGet(() -> {
                    String groupName = (society.getName() != null && !society.getName().isBlank())
                            ? society.getName() + " Community Group"
                            : "Society Community Group";
                    String desc = "Official residential community group for " +
                            (society.getName() != null ? society.getName() : "the society");

                    SocietyGroup group = SocietyGroup.builder()
                            .societyId(society.getId())
                            .name(groupName)
                            .description(desc)
                            .memberUserIds(new HashSet<>())
                            .createdAutomatically(true)
                            .build();

                    SocietyGroup saved = societyGroupRepository.save(group);
                    log.info("Automatic society group created for society {}: {}", society.getName(), saved.getId());
                    return saved;
                });
    }

    /**
     * Automatically joins a resident to their society's official group after onboarding.
     * Enforces the multi-group rule: Only PLATFORM_ADMIN can be in multiple groups.
     */
    public void autoJoinResident(String societyId, String userId) {
        if (societyId == null || societyId.isBlank() || userId == null || userId.isBlank()) {
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Cannot auto-join null user {} to group for society {}", userId, societyId);
            return;
        }

        // Multi-group constraint check: Non-platform admins can belong to at most 1 society group.
        if (user.getPlatformRole() != PlatformRole.PLATFORM_ADMIN) {
            List<SocietyGroup> existingGroups = societyGroupRepository.findByMemberUserIdsContaining(userId);
            boolean alreadyInOtherGroup = existingGroups.stream()
                    .anyMatch(g -> !societyId.equals(g.getSocietyId()));
            if (alreadyInOtherGroup) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Residents and society admins can belong to only 1 society group. Multi-group access is reserved for platform administrators."
                );
            }
        }

        Society society = societyRepository.findById(societyId).orElse(null);
        SocietyGroup group = (society != null)
                ? createOrGetGroupForSociety(society)
                : societyGroupRepository.findBySocietyId(societyId).orElseGet(() -> {
                    SocietyGroup g = SocietyGroup.builder()
                            .societyId(societyId)
                            .name("Society Community Group")
                            .description("Official residential group")
                            .memberUserIds(new HashSet<>())
                            .createdAutomatically(true)
                            .build();
                    return societyGroupRepository.save(g);
                });

        if (group.getMemberUserIds() == null) {
            group.setMemberUserIds(new HashSet<>());
        }

        if (group.getMemberUserIds().add(userId)) {
            societyGroupRepository.save(group);
            log.info("User {} automatically joined society group {} for society {}",
                    user.getEmail(), group.getId(), societyId);
        }
    }

    /**
     * Retrieves the single official group for the caller's society.
     */
    public SocietyGroupResponse getMySocietyGroup(User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        String societyId = TenantContext.getSocietyId();
        if (societyId == null || societyId.isBlank()) {
            societyId = currentUser.getSocietyId();
        }

        if (societyId == null || societyId.isBlank()) {
            if (currentUser.getPlatformRole() == PlatformRole.PLATFORM_ADMIN) {
                // Platform admin with no active tenant context: return first group or empty
                List<SocietyGroup> all = societyGroupRepository.findAll();
                if (!all.isEmpty()) {
                    return toResponse(all.get(0), currentUser);
                }
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No society groups found on platform");
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No society context found for user");
        }

        String finalSocietyId = societyId;
        SocietyGroup group = societyGroupRepository.findBySocietyId(finalSocietyId)
                .orElseGet(() -> {
                    Society society = societyRepository.findById(finalSocietyId).orElse(null);
                    if (society != null) {
                        return createOrGetGroupForSociety(society);
                    }
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Society group not found for society: " + finalSocietyId);
                });

        return toResponse(group, currentUser);
    }

    /**
     * Retrieves a group by ID. Regular residents/admins can only access their own society's group.
     * Platform admins can access any society group.
     */
    public SocietyGroupResponse getGroupById(String groupId, User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }

        SocietyGroup group = societyGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found: " + groupId));

        if (currentUser.getPlatformRole() != PlatformRole.PLATFORM_ADMIN) {
            String userSocietyId = currentUser.getSocietyId();
            if (userSocietyId == null || !userSocietyId.equals(group.getSocietyId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied to other society's group");
            }
        }

        return toResponse(group, currentUser);
    }

    /**
     * Lists all society groups. Restricted to PLATFORM_ADMIN.
     */
    public List<SocietyGroupResponse> getAllGroupsForPlatformAdmin(User currentUser) {
        if (currentUser == null || currentUser.getPlatformRole() != PlatformRole.PLATFORM_ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only platform administrators can list all platform groups");
        }

        return societyGroupRepository.findAll().stream()
                .map(g -> toResponse(g, currentUser))
                .toList();
    }

    /**
     * Manually adds a member to a society group.
     * STRICT RULE: Except platform admin, no one can manually add anyone to a group.
     */
    public SocietyGroupResponse manualAddMember(String groupId, String targetUserId, User currentUser) {
        if (currentUser == null || currentUser.getPlatformRole() != PlatformRole.PLATFORM_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only platform administrators can manually add members to a society group. Group membership is managed automatically by the onboarding engine."
            );
        }

        SocietyGroup group = societyGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found: " + groupId));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found: " + targetUserId));

        // Enforce multi-group rule: Non-platform admins cannot be added to a second society group.
        if (targetUser.getPlatformRole() != PlatformRole.PLATFORM_ADMIN) {
            List<SocietyGroup> existingGroups = societyGroupRepository.findByMemberUserIdsContaining(targetUserId);
            boolean inAnotherGroup = existingGroups.stream()
                    .anyMatch(g -> !g.getId().equals(group.getId()));
            if (inAnotherGroup) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Target resident already belongs to another society group. Only platform administrators can belong to multiple groups."
                );
            }
        }

        if (group.getMemberUserIds() == null) {
            group.setMemberUserIds(new HashSet<>());
        }

        group.getMemberUserIds().add(targetUserId);
        SocietyGroup saved = societyGroupRepository.save(group);
        log.info("Platform admin {} manually added user {} to group {}",
                currentUser.getEmail(), targetUser.getEmail(), groupId);

        return toResponse(saved, currentUser);
    }

    /**
     * Manually removes a member from a society group.
     * STRICT RULE: Except platform admin, no one can remove anyone from a group.
     */
    public SocietyGroupResponse manualRemoveMember(String groupId, String targetUserId, User currentUser) {
        if (currentUser == null || currentUser.getPlatformRole() != PlatformRole.PLATFORM_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only platform administrators can remove members from a society group."
            );
        }

        SocietyGroup group = societyGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found: " + groupId));

        if (group.getMemberUserIds() != null && group.getMemberUserIds().remove(targetUserId)) {
            SocietyGroup saved = societyGroupRepository.save(group);
            log.info("Platform admin {} manually removed user {} from group {}",
                    currentUser.getEmail(), targetUserId, groupId);
            return toResponse(saved, currentUser);
        }

        return toResponse(group, currentUser);
    }

    /**
     * Removes a user from all groups upon platform deletion by platform admin.
     */
    public void removeUserFromAllGroups(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        List<SocietyGroup> groups = societyGroupRepository.findByMemberUserIdsContaining(userId);
        for (SocietyGroup g : groups) {
            if (g.getMemberUserIds() != null && g.getMemberUserIds().remove(userId)) {
                societyGroupRepository.save(g);
                log.info("Removed user {} from group {}", userId, g.getId());
            }
        }
    }

    public SocietyGroupResponse toResponse(SocietyGroup group) {
        return toResponse(group, null);
    }

    public SocietyGroupResponse toResponse(SocietyGroup group, User currentUser) {
        Set<String> memberIds = group.getMemberUserIds() != null ? group.getMemberUserIds() : Collections.emptySet();
        List<User> users = userRepository.findAllById(memberIds);

        Map<String, String> flatMap = Collections.emptyMap();
        List<String> flatIds = users.stream()
                .map(User::getFlatId)
                .filter(Objects::nonNull)
                .toList();

        if (!flatIds.isEmpty()) {
            flatMap = flatRepository.findAllById(flatIds).stream()
                    .collect(Collectors.toMap(
                            Flat::getId,
                            f -> (f.getBlock() != null ? f.getBlock() + "-" : "") + f.getFlatNumber(),
                            (existing, replacement) -> existing
                    ));
        }

        boolean privileged = currentUser != null && (
                currentUser.getPlatformRole() == PlatformRole.PLATFORM_ADMIN ||
                currentUser.getRole() == Roles.ADMIN ||
                currentUser.getRole() == Roles.SUPER_ADMIN
        );

        Map<String, String> finalFlatMap = flatMap;
        List<GroupMemberDto> memberDtos = users.stream()
                .map(u -> {
                    String fullName = ((u.getFirstName() != null ? u.getFirstName() : "") + " " +
                            (u.getLastName() != null ? u.getLastName() : "")).trim();
                    boolean isSelf = currentUser != null && u.getId() != null && u.getId().equals(currentUser.getId());
                    boolean canViewSensitive = privileged || isSelf;

                    String displayEmail = canViewSensitive ? u.getEmail() : FlatResponse.maskEmail(u.getEmail());
                    String displayPhone = canViewSensitive ? u.getPhone() : FlatResponse.maskPhone(u.getPhone());

                    return GroupMemberDto.builder()
                            .userId(u.getId())
                            .name(fullName.isBlank() ? (displayEmail != null ? displayEmail : "Resident") : fullName)
                            .email(displayEmail)
                            .phone(displayPhone)
                            .role(u.getRole() != null ? u.getRole().name() : "MEMBER")
                            .platformRole(u.getPlatformRole() != null ? u.getPlatformRole().name() : "SOCIETY_USER")
                            .flatId(u.getFlatId())
                            .flatNumber(u.getFlatId() != null ? finalFlatMap.getOrDefault(u.getFlatId(), "N/A") : "N/A")
                            .isOwner(u.isOwner())
                            .build();
                })
                .sorted(Comparator.comparing(GroupMemberDto::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        return SocietyGroupResponse.builder()
                .id(group.getId())
                .societyId(group.getSocietyId())
                .name(group.getName())
                .description(group.getDescription())
                .memberCount(memberDtos.size())
                .members(memberDtos)
                .createdAutomatically(group.isCreatedAutomatically())
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }
}
