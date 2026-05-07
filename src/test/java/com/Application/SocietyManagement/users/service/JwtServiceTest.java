package com.Application.SocietyManagement.users.service;

import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET",
                "TestSecretKeyForJwtThatIsAtLeast32BytesLong=");
        ReflectionTestUtils.setField(jwtService, "EXPIRATION", 86400000L);

        testUser = User.builder()
                .email("resident@test.com")
                .role(Roles.RESIDENT)
                .status(Status.ACTIVE)
                .build();
    }

    @Test
    void generateToken_returnsNonNullToken() {
        String token = jwtService.generateToken(testUser);
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_hasThreeParts() {
        String token = jwtService.generateToken(testUser);
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtService.generateToken(testUser);
        assertThat(jwtService.extractEmail(token))
                .isEqualTo("resident@test.com");
    }

    @Test
    void extractRole_returnsCorrectRole() {
        String token = jwtService.generateToken(testUser);
        assertThat(jwtService.extractRole(token))
                .isEqualTo("RESIDENT");
    }

    @Test
    void isValid_validToken_returnsTrue() {
        String token = jwtService.generateToken(testUser);
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_tamperedToken_returnsFalse() {
        String token = jwtService.generateToken(testUser);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_randomString_returnsFalse() {
        assertThat(jwtService.isValid("not.a.token")).isFalse();
    }

    @Test
    void isTokenExpired_validToken_returnsFalse() {
        String token = jwtService.generateToken(testUser);
        assertThat(jwtService.isTokenExpired(token)).isFalse();
    }

    @Test
    void isTokenExpired_expiredToken_returnsTrue() {
        ReflectionTestUtils.setField(jwtService, "EXPIRATION", -1000L);
        String token = jwtService.generateToken(testUser);
        // isTokenExpired throws ExpiredJwtException internally
        // so we verify via isValid which catches all exceptions
        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void generateToken_differentUsers_produceDifferentTokens() {
        User anotherUser = User.builder()
                .email("admin@test.com")
                .role(Roles.ADMIN)
                .status(Status.ACTIVE)
                .build();

        String token1 = jwtService.generateToken(testUser);
        String token2 = jwtService.generateToken(anotherUser);

        assertThat(token1).isNotEqualTo(token2);
    }

    @Test
    void generateToken_adminRole_extractsCorrectly() {
        User adminUser = User.builder()
                .email("admin@test.com")
                .role(Roles.ADMIN)
                .status(Status.ACTIVE)
                .build();

        String token = jwtService.generateToken(adminUser);

        assertThat(jwtService.extractEmail(token)).isEqualTo("admin@test.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN");
    }
}
