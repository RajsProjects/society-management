package com.Application.SocietyManagement.communication.service;

import com.Application.SocietyManagement.communication.dto.SocietyGroupResponse;
import com.Application.SocietyManagement.communication.entity.SocietyGroup;
import com.Application.SocietyManagement.communication.repository.SocietyGroupRepository;
import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.flat.repository.FlatRepository;
import com.Application.SocietyManagement.society.entity.Society;
import com.Application.SocietyManagement.society.repository.SocietyRepository;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.PlatformRole;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SocietyGroupService")
class SocietyGroupServiceTest {

    @Mock
    private SocietyGroupRepository societyGroupRepository;

    @Mock
    private SocietyRepository societyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FlatRepository flatRepository;

    @InjectMocks
    private SocietyGroupService societyGroupService;

    private Society society;
    private SocietyGroup societyGroup;
    private User residentUser;
    private User societyAdminUser;
    private User platformAdminUser;

    @BeforeEach
    void setUp() {
        TenantContext.setSocietyId("soc-1");

        society = Society.builder()
                .name("Green Valley Society")
                .build();
        society.setId("soc-1");

        societyGroup = SocietyGroup.builder()
                .societyId("soc-1")
                .name("Green Valley Community Group")
                .description("Official residential group")
                .memberUserIds(new HashSet<>(List.of("user-1")))
                .createdAutomatically(true)
                .build();
        societyGroup.setId("grp-1");

        residentUser = User.builder()
                .email("resident@greenvalley.com")
                .firstName("John")
                .lastName("Doe")
                .societyId("soc-1")
                .role(Roles.RESIDENT)
                .platformRole(PlatformRole.SOCIETY_USER)
                .build();
        residentUser.setId("user-1");

        societyAdminUser = User.builder()
                .email("admin@greenvalley.com")
                .firstName("Alice")
                .lastName("Admin")
                .societyId("soc-1")
                .role(Roles.ADMIN)
                .platformRole(PlatformRole.SOCIETY_USER)
                .build();
        societyAdminUser.setId("admin-1");

        platformAdminUser = User.builder()
                .email("super@platform.gov")
                .firstName("Platform")
                .lastName("Admin")
                .platformRole(PlatformRole.PLATFORM_ADMIN)
                .build();
        platformAdminUser.setId("platform-admin-id");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("createOrGetGroupForSociety")
    class CreateOrGetGroup {

        @Test
        @DisplayName("Returns existing group if already present (1 group per society rule)")
        void returnsExistingGroup() {
            when(societyGroupRepository.findBySocietyId("soc-1")).thenReturn(Optional.of(societyGroup));

            SocietyGroup result = societyGroupService.createOrGetGroupForSociety(society);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("grp-1");
            verify(societyGroupRepository, never()).save(any());
        }

        @Test
        @DisplayName("Creates new group if not present")
        void createsNewGroupIfNotPresent() {
            when(societyGroupRepository.findBySocietyId("soc-1")).thenReturn(Optional.empty());
            when(societyGroupRepository.save(any(SocietyGroup.class))).thenAnswer(inv -> {
                SocietyGroup g = inv.getArgument(0);
                g.setId("new-grp-id");
                return g;
            });

            SocietyGroup result = societyGroupService.createOrGetGroupForSociety(society);

            assertThat(result).isNotNull();
            assertThat(result.getName()).contains("Green Valley");
            assertThat(result.isCreatedAutomatically()).isTrue();
            verify(societyGroupRepository).save(any(SocietyGroup.class));
        }
    }

    @Nested
    @DisplayName("autoJoinResident")
    class AutoJoinResident {

        @Test
        @DisplayName("Automatically joins resident to their society group")
        void autoJoinsResidentSuccessfully() {
            when(userRepository.findById("user-2")).thenReturn(Optional.of(
                    User.builder()
                            .email("user2@test.com")
                            .platformRole(PlatformRole.SOCIETY_USER)
                            .societyId("soc-1")
                            .build()
            ));
            when(societyGroupRepository.findByMemberUserIdsContaining("user-2")).thenReturn(Collections.emptyList());
            when(societyRepository.findById("soc-1")).thenReturn(Optional.of(society));
            when(societyGroupRepository.findBySocietyId("soc-1")).thenReturn(Optional.of(societyGroup));
            when(societyGroupRepository.save(any(SocietyGroup.class))).thenAnswer(inv -> inv.getArgument(0));

            societyGroupService.autoJoinResident("soc-1", "user-2");

            assertThat(societyGroup.getMemberUserIds()).contains("user-2");
            verify(societyGroupRepository).save(societyGroup);
        }

        @Test
        @DisplayName("Throws 403 Forbidden if non-platform admin resident is already in another society group")
        void throwsForbiddenIfResidentInAnotherGroup() {
            SocietyGroup otherGroup = SocietyGroup.builder()
                    .societyId("soc-other")
                    .build();
            otherGroup.setId("grp-other");

            when(userRepository.findById("user-multi")).thenReturn(Optional.of(
                    User.builder()
                            .email("multi@test.com")
                            .platformRole(PlatformRole.SOCIETY_USER)
                            .societyId("soc-1")
                            .build()
            ));
            when(societyGroupRepository.findByMemberUserIdsContaining("user-multi")).thenReturn(List.of(otherGroup));

            assertThatThrownBy(() -> societyGroupService.autoJoinResident("soc-1", "user-multi"))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(403);
                        assertThat(rse.getReason()).contains("Multi-group access is reserved for platform administrators");
                    });
        }
    }

    @Nested
    @DisplayName("Manual Member Management Boundaries")
    class ManualMemberManagement {

        @Test
        @DisplayName("manualAddMember - PLATFORM_ADMIN can manually add member")
        void manualAddMember_platformAdmin_success() {
            when(societyGroupRepository.findById("grp-1")).thenReturn(Optional.of(societyGroup));

            User targetUser = User.builder()
                    .email("target@test.com")
                    .platformRole(PlatformRole.SOCIETY_USER)
                    .build();
            targetUser.setId("target-1");

            when(userRepository.findById("target-1")).thenReturn(Optional.of(targetUser));
            when(societyGroupRepository.findByMemberUserIdsContaining("target-1")).thenReturn(Collections.emptyList());
            when(societyGroupRepository.save(any(SocietyGroup.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findAllById(any())).thenReturn(List.of(residentUser, targetUser));

            SocietyGroupResponse res = societyGroupService.manualAddMember("grp-1", "target-1", platformAdminUser);

            assertThat(res).isNotNull();
            assertThat(societyGroup.getMemberUserIds()).contains("target-1");
            verify(societyGroupRepository).save(societyGroup);
        }

        @Test
        @DisplayName("manualAddMember - Throws 403 Forbidden if called by society admin")
        void manualAddMember_societyAdmin_throwsForbidden() {
            assertThatThrownBy(() -> societyGroupService.manualAddMember("grp-1", "any-user", societyAdminUser))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(403);
                        assertThat(rse.getReason()).contains("Only platform administrators can manually add members");
                    });
            verify(societyGroupRepository, never()).save(any());
        }

        @Test
        @DisplayName("manualRemoveMember - PLATFORM_ADMIN can manually remove member")
        void manualRemoveMember_platformAdmin_success() {
            when(societyGroupRepository.findById("grp-1")).thenReturn(Optional.of(societyGroup));
            when(societyGroupRepository.save(any(SocietyGroup.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findAllById(any())).thenReturn(Collections.emptyList());

            SocietyGroupResponse res = societyGroupService.manualRemoveMember("grp-1", "user-1", platformAdminUser);

            assertThat(res).isNotNull();
            assertThat(societyGroup.getMemberUserIds()).doesNotContain("user-1");
            verify(societyGroupRepository).save(societyGroup);
        }

        @Test
        @DisplayName("manualRemoveMember - Throws 403 Forbidden if called by society admin or resident")
        void manualRemoveMember_societyAdmin_throwsForbidden() {
            assertThatThrownBy(() -> societyGroupService.manualRemoveMember("grp-1", "user-1", societyAdminUser))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode().value()).isEqualTo(403);
                        assertThat(rse.getReason()).contains("Only platform administrators can remove members");
                    });
            verify(societyGroupRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeUserFromAllGroups")
    class RemoveUserFromAllGroups {

        @Test
        @DisplayName("Removes user from any society group upon platform deletion")
        void removesUserFromAllGroups() {
            when(societyGroupRepository.findByMemberUserIdsContaining("user-1")).thenReturn(List.of(societyGroup));
            when(societyGroupRepository.save(any(SocietyGroup.class))).thenAnswer(inv -> inv.getArgument(0));

            societyGroupService.removeUserFromAllGroups("user-1");

            assertThat(societyGroup.getMemberUserIds()).doesNotContain("user-1");
            verify(societyGroupRepository).save(societyGroup);
        }
    }
}
