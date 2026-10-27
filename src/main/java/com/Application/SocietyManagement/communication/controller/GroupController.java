package com.Application.SocietyManagement.communication.controller;

import com.Application.SocietyManagement.communication.dto.ManualGroupMemberRequest;
import com.Application.SocietyManagement.communication.dto.SocietyGroupResponse;
import com.Application.SocietyManagement.communication.service.SocietyGroupService;
import com.Application.SocietyManagement.users.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
@Tag(name = "Society Groups", description = "Operations for the single official community group per society")
public class GroupController {

    private final SocietyGroupService societyGroupService;

    @Operation(
            summary = "Get My Society Group",
            description = "Returns the single official community group for the caller's society, including member listings."
    )
    @GetMapping("/my-group")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SocietyGroupResponse> getMyGroup(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(societyGroupService.getMySocietyGroup(currentUser));
    }

    @Operation(
            summary = "Get Society Group by ID",
            description = "Returns details of a society group. Residents can only access their own society's group. Platform admin can access any group."
    )
    @GetMapping("/{groupId}")
    @PreAuthorize("hasAnyRole('RESIDENT', 'ADMIN', 'SUPER_ADMIN', 'PLATFORM_ADMIN')")
    public ResponseEntity<SocietyGroupResponse> getGroupById(
            @PathVariable String groupId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(societyGroupService.getGroupById(groupId, currentUser));
    }

    @Operation(
            summary = "List All Society Groups (Platform Admin Only)",
            description = "Returns all registered society groups across the platform. Multi-group visibility is exclusive to platform administrators."
    )
    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<List<SocietyGroupResponse>> getAllGroups(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(societyGroupService.getAllGroupsForPlatformAdmin(currentUser));
    }

    @Operation(
            summary = "Manually Add Member to Group (Platform Admin Only)",
            description = "Except platform admin, no one can manually add members to a society group; residents are automatically joined on onboarding."
    )
    @PostMapping("/{groupId}/members")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<SocietyGroupResponse> addMember(
            @PathVariable String groupId,
            @RequestBody @Valid ManualGroupMemberRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(societyGroupService.manualAddMember(groupId, request.getUserId(), currentUser));
    }

    @Operation(
            summary = "Manually Remove Member from Group (Platform Admin Only)",
            description = "Except platform admin, no one can remove any member from a society group."
    )
    @DeleteMapping("/{groupId}/members/{userId}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ResponseEntity<SocietyGroupResponse> removeMember(
            @PathVariable String groupId,
            @PathVariable String userId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(societyGroupService.manualRemoveMember(groupId, userId, currentUser));
    }
}
