package com.Application.SocietyManagement.users.service;

import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;
    private User residentUser;
    private User adminUser;
    private User superAdminUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET",
                "TestSecretKeyForJwtThatIsAtLeast32BytesLong=");
        ReflectionTestUtils.setField(jwtService, "EXPIRATION", 86400000L);

        residentUser = User.builder()
                .email("resident@test.com")
                .role(Roles.RESIDENT)
                .status(Status.ACTIVE)
                .societyId("society-001")
                .build();

        adminUser = User.builder()
                .email("admin@test.com")
                .role(Roles.ADMIN)
                .status(Status.ACTIVE)
                .societyId("society-001")
                .build();

        superAdminUser = User.builder()
                .email("superadmin@test.com")
                .role(Roles.SUPER_ADMIN)
                .status(Status.ACTIVE)
                .societyId("society-001")
                .build();
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("returns non-null token")
        void generateToken_returnsNonNullToken() {
            String token = jwtService.generateToken(residentUser);
            assertThat(token).isNotNull().isNotBlank();
        }

        @Test
        @DisplayName("token has three parts (header.payload.signature)")
        void generateToken_hasThreeParts() {
            String token = jwtService.generateToken(residentUser);
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("different users produce different tokens")
        void generateToken_differentUsers_produceDifferentTokens() {
            String token1 = jwtService.generateToken(residentUser);
            String token2 = jwtService.generateToken(adminUser);
            assertThat(token1).isNotEqualTo(token2);
        }

        @Test
        @DisplayName("same user produces different tokens (timestamp diff)")
        void generateToken_sameUserTwice_producesUniqueTokens()
                throws InterruptedException {
            String token1 = jwtService.generateToken(residentUser);
            Thread.sleep(10);
            String token2 = jwtService.generateToken(residentUser);
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    @Nested
    @DisplayName("extractEmail")
    class ExtractEmail {

        @Test
        @DisplayName("extracts correct email for resident")
        void extractEmail_resident_returnsCorrectEmail() {
            String token = jwtService.generateToken(residentUser);
            assertThat(jwtService.extractEmail(token))
                    .isEqualTo("resident@test.com");
        }

        @Test
        @DisplayName("extracts correct email for admin")
        void extractEmail_admin_returnsCorrectEmail() {
            String token = jwtService.generateToken(adminUser);
            assertThat(jwtService.extractEmail(token))
                    .isEqualTo("admin@test.com");
        }
    }

    @Nested
    @DisplayName("extractRole")
    class ExtractRole {

        @Test
        @DisplayName("extracts RESIDENT role correctly")
        void extractRole_resident_returnsResident() {
            String token = jwtService.generateToken(residentUser);
            assertThat(jwtService.extractRole(token)).isEqualTo("RESIDENT");
        }

        @Test
        @DisplayName("extracts ADMIN role correctly")
        void extractRole_admin_returnsAdmin() {
            String token = jwtService.generateToken(adminUser);
            assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
        }

        @Test
        @DisplayName("extracts SUPER_ADMIN role correctly")
        void extractRole_superAdmin_returnsSuperAdmin() {
            String token = jwtService.generateToken(superAdminUser);
            assertThat(jwtService.extractRole(token)).isEqualTo("SUPER_ADMIN");
        }
    }

    @Nested
    @DisplayName("extractSocietyId")
    class ExtractSocietyId {

        @Test
        @DisplayName("extracts societyId correctly")
        void extractSocietyId_returnsCorrectSocietyId() {
            String token = jwtService.generateToken(residentUser);
            assertThat(jwtService.extractSocietyId(token))
                    .isEqualTo("society-001");
        }

        @Test
        @DisplayName("different societies produce different societyId claims")
        void extractSocietyId_differentSocieties_returnsDifferent() {
            User societyBUser = User.builder()
                    .email("user@b.com")
                    .role(Roles.RESIDENT)
                    .status(Status.ACTIVE)
                    .societyId("society-002")
                    .build();

            String tokenA = jwtService.generateToken(residentUser);
            String tokenB = jwtService.generateToken(societyBUser);

            assertThat(jwtService.extractSocietyId(tokenA)).isEqualTo("society-001");
            assertThat(jwtService.extractSocietyId(tokenB)).isEqualTo("society-002");
        }
    }

    @Nested
    @DisplayName("isValid")
    class IsValid {

        @Test
        @DisplayName("valid token - returns true")
        void isValid_validToken_returnsTrue() {
            String token = jwtService.generateToken(residentUser);
            assertThat(jwtService.isValid(token)).isTrue();
        }

        @Test
        @DisplayName("tampered signature - returns false")
        void isValid_tamperedToken_returnsFalse() {
            String token = jwtService.generateToken(residentUser);
            String tampered = token.substring(0, token.length() - 5) + "XXXXX";
            assertThat(jwtService.isValid(tampered)).isFalse();
        }

        @Test
        @DisplayName("random string - returns false")
        void isValid_randomString_returnsFalse() {
            assertThat(jwtService.isValid("not.a.token")).isFalse();
        }

        @Test
        @DisplayName("empty string - returns false")
        void isValid_emptyString_returnsFalse() {
            assertThat(jwtService.isValid("")).isFalse();
        }

        @Test
        @DisplayName("expired token - returns false")
        void isValid_expiredToken_returnsFalse() {
            ReflectionTestUtils.setField(jwtService, "EXPIRATION", -1000L);
            String token = jwtService.generateToken(residentUser);
            assertThat(jwtService.isValid(token)).isFalse();
        }

        @Test
        @DisplayName("token signed with different secret - returns false")
        void isValid_differentSecret_returnsFalse() {
            String token = jwtService.generateToken(residentUser);

            JwtService otherService = new JwtService();
            ReflectionTestUtils.setField(otherService, "SECRET",
                    "CompletelyDifferentSecretKeyThatIsAlso32Bytes=");
            ReflectionTestUtils.setField(otherService, "EXPIRATION", 86400000L);

            assertThat(otherService.isValid(token)).isFalse();
        }
    }

    @Nested
    @DisplayName("isTokenExpired")
    class IsTokenExpired {

        @Test
        @DisplayName("valid token - not expired")
        void isTokenExpired_validToken_returnsFalse() {
            String token = jwtService.generateToken(residentUser);
            assertThat(jwtService.isTokenExpired(token)).isFalse();
        }

        @Test
        @DisplayName("expired token - isValid returns false")
        void isTokenExpired_expiredToken_isValidReturnsFalse() {
            ReflectionTestUtils.setField(jwtService, "EXPIRATION", -1000L);
            String token = jwtService.generateToken(residentUser);
            assertThat(jwtService.isValid(token)).isFalse();
        }
    }
}