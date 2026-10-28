package com.Application.SocietyManagement.core.security;

import com.Application.SocietyManagement.flat.dto.FlatResponse;
import com.Application.SocietyManagement.communication.dto.SocietyGroupResponse;
import com.Application.SocietyManagement.communication.entity.SocietyGroup;
import com.Application.SocietyManagement.communication.service.SocietyGroupService;
import com.Application.SocietyManagement.core.config.SecurityConfig;
import com.Application.SocietyManagement.society.dto.SocietyResponse;
import com.Application.SocietyManagement.society.entity.Society;
import com.Application.SocietyManagement.users.dto.UserResponseDto;
import com.Application.SocietyManagement.users.dto.UserSummarydto;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.PlatformRole;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security: Information Leakage, Cryptography & Security Headers Test Suite")
class InformationLeakageSecurityTest {

        @Mock
        private UserRepository userRepository;

        @Test
        @DisplayName("Password Storage: Password encoder uses peppered BCrypt and never stores plaintext")
        void passwordEncoder_hashesWithPepperedBcrypt() {
                PasswordEncoder encoder = new PepperedPasswordEncoder("super-secret-server-side-pepper-key");
                String rawPassword = "StrongUserPassword1!";
                String encoded = encoder.encode(rawPassword);

                assertThat(encoded).isNotBlank();
                assertThat(encoded).doesNotContain(rawPassword);
                assertThat(encoded).startsWith("$2a$"); // BCrypt salt prefix
                assertThat(encoder.matches(rawPassword, encoded)).isTrue();
                assertThat(encoder.matches("wrongPassword", encoded)).isFalse();
        }

        @Test
        @DisplayName("DTO Sanitization: UserResponseDto and UserSummarydto contain no password fields")
        void userDtos_containNoPasswordFields() {
                List<String> userResponseFields = Arrays.stream(UserResponseDto.class.getDeclaredFields())
                                .map(Field::getName)
                                .map(String::toLowerCase)
                                .toList();

                List<String> userSummaryFields = Arrays.stream(UserSummarydto.class.getDeclaredFields())
                                .map(Field::getName)
                                .map(String::toLowerCase)
                                .toList();

                assertThat(userResponseFields)
                                .doesNotContain("password", "passwordhash", "secret", "pepper");

                assertThat(userSummaryFields)
                                .doesNotContain("password", "passwordhash", "secret", "pepper");
        }

        @Test
        @DisplayName("CORS Hardening: CORS configuration uses explicit origins and strictly rejects wildcard *")
        void corsConfig_restrictsOrigins() {
                SecurityConfig config = new SecurityConfig(null, null, null, null);
                ReflectionTestUtils.setField(config, "allowedOrigins",
                                "https://app.civiclink.in,https://admin.civiclink.in");

                CorsConfigurationSource source = config.corsConfigurationSource();
                MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
                CorsConfiguration cors = source.getCorsConfiguration(request);

                assertThat(cors).isNotNull();
                assertThat(cors.getAllowedOriginPatterns())
                                .containsExactly("https://app.civiclink.in", "https://admin.civiclink.in")
                                .doesNotContain("*");
        }

        @Test
        @DisplayName("PII Protection: Phone and email masking utility correctly masks sensitive substrings")
        void piiMaskingUtility_masksPhoneAndEmail() {
                String email = "superadmin@society.com";
                String maskedEmail = FlatResponse.maskEmail(email);

                assertThat(maskedEmail).isEqualTo("s***@society.com");
                assertThat(maskedEmail).doesNotContain("superadmin");

                String phone = "9876543210";
                String maskedPhone = FlatResponse.maskPhone(phone);

                assertThat(maskedPhone).isEqualTo("******3210");
                assertThat(maskedPhone).doesNotContain("987654");
        }

        @Test
        @DisplayName("PII Protection: Group member phone and email are masked for standard RESIDENT view")
        void societyGroupService_masksPiiForResident() {
                SocietyGroupService groupService = new SocietyGroupService(null, null, userRepository, null);

                User residentCaller = User.builder()
                                .email("caller@soc.com")
                                .phone("9111111111")
                                .role(Roles.RESIDENT)
                                .platformRole(PlatformRole.SOCIETY_USER)
                                .societyId("soc-1")
                                .build();
                residentCaller.setId("caller-id");

                User targetUser = User.builder()
                                .email("committee@soc.com")
                                .phone("9876543210")
                                .role(Roles.ADMIN)
                                .platformRole(PlatformRole.SOCIETY_USER)
                                .societyId("soc-1")
                                .build();
                targetUser.setId("target-id");

                SocietyGroup group = SocietyGroup.builder()
                                .societyId("soc-1")
                                .name("General Community")
                                .memberUserIds(Set.of("target-id"))
                                .build();
                group.setId("group-1");

                when(userRepository.findAllById(any())).thenReturn(List.of(targetUser));

                SocietyGroupResponse response = groupService.toResponse(group, residentCaller);

                assertThat(response.getMembers()).hasSize(1);
                var member = response.getMembers().get(0);
                assertThat(member.getEmail()).isEqualTo("c***@soc.com");
                assertThat(member.getPhone()).isEqualTo("******3210");
        }

        @Test
        @DisplayName("Tenant Secret Protection: SocietyResponse masks joinCode and documentUrl for standard residents")
        void societyResponse_masksJoinCodeForResident() {
                Society society = Society.builder()
                                .name("Palm Meadows")
                                .joinCode("SECRET-JOIN-123")
                                .societyCode("PM-4001")
                                .documentUrl("society-documents/confidential-deed.pdf")
                                .status(com.Application.SocietyManagement.society.enums.SocietyStatus.ACTIVE)
                                .build();
                society.setId("soc-10");

                // When viewed by non-privileged resident (privileged = false)
                SocietyResponse residentView = SocietyResponse.from(society, false);
                assertThat(residentView.getJoinCode()).isNull();
                assertThat(residentView.getDocumentUrl()).isNull();

                // When viewed by society admin (privileged = true)
                SocietyResponse adminView = SocietyResponse.from(society, true);
                assertThat(adminView.getJoinCode()).isEqualTo("SECRET-JOIN-123");
                assertThat(adminView.getDocumentUrl()).isEqualTo("society-documents/confidential-deed.pdf");
        }
}
