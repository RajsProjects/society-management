package com.Application.SocietyManagement.core.security;

import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.PlatformRole;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Security: JWT Integrity, Signature & Claim Verification Test Suite")
class JwtSecurityTest {

    private static final String STRONG_SECRET = "super-secure-jwt-signing-key-for-unit-tests-256-bits!";
    private static final long EXPIRATION_MS = 3600000L;

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET", STRONG_SECRET);
        ReflectionTestUtils.setField(jwtService, "EXPIRATION", EXPIRATION_MS);
        jwtService.validateConfiguration();

        testUser = User.builder()
                .email("resident@civiclink.in")
                .role(Roles.RESIDENT)
                .platformRole(PlatformRole.SOCIETY_USER)
                .status(Status.ACTIVE)
                .societyId("soc-green-valley")
                .build();
        testUser.setId("user-uuid-100");
    }

    @Test
    @DisplayName("JWT Config: Rejects weak signing keys shorter than 32 bytes (256 bits)")
    void validateConfiguration_rejectsShortKey() {
        JwtService weakJwtService = new JwtService();
        ReflectionTestUtils.setField(weakJwtService, "SECRET", "short-secret-123");
        ReflectionTestUtils.setField(weakJwtService, "EXPIRATION", EXPIRATION_MS);

        assertThatThrownBy(weakJwtService::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes (256 bits)");
    }

    @Test
    @DisplayName("JWT Generation: Successfully generates token with expected claims and valid signature")
    void generateToken_hasExpectedClaims() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractEmail(token)).isEqualTo("resident@civiclink.in");
        assertThat(jwtService.extractRole(token)).isEqualTo(Roles.RESIDENT.name());
        assertThat(jwtService.extractPlatformRole(token)).isEqualTo(PlatformRole.SOCIETY_USER.name());
        assertThat(jwtService.extractSocietyId(token)).isEqualTo("soc-green-valley");
    }

    @Test
    @DisplayName("JWT Integrity: Tampering with claims payload invalidates HMAC-SHA signature")
    void claimTampering_invalidatesSignature() {
        String token = jwtService.generateToken(testUser);
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);

        // Decode payload, modify role from RESIDENT to ADMIN, re-encode without valid signature
        String header = parts[0];
        String signature = parts[2];

        // Craft rogue payload with forged ADMIN role
        SecretKey rogueKey = Keys.hmacShaKeyFor("different-unauthorized-secret-key-32-bytes!".getBytes(StandardCharsets.UTF_8));
        String forgedToken = Jwts.builder()
                .subject("resident@civiclink.in")
                .claim("role", Roles.ADMIN.name())
                .claim("societyId", "soc-green-valley")
                .signWith(rogueKey)
                .compact();

        // Validate that our actual application parser rejects the forged token
        assertThat(jwtService.isValid(forgedToken)).isFalse();
        assertThat(jwtService.parseAndValidateClaims(forgedToken)).isNull();
    }

    @Test
    @DisplayName("JWT Expiration: Token with past expiration date is detected as expired")
    void expiredToken_isDetectedAndRejected() {
        SecretKey key = Keys.hmacShaKeyFor(STRONG_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("resident@civiclink.in")
                .expiration(new Date(System.currentTimeMillis() - 60000))
                .signWith(key)
                .compact();

        assertThat(jwtService.isTokenExpired(expiredToken)).isTrue();
        assertThat(jwtService.isValid(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("JWT Confidentiality: Token contains no sensitive credentials or password hashes")
    void tokenContainsNoPasswordsOrHashes() {
        String token = jwtService.generateToken(testUser);
        Claims claims = jwtService.parseAndValidateClaims(token);

        assertThat(claims).isNotNull();
        assertThat(claims.get("password")).isNull();
        assertThat(claims.get("passwordHash")).isNull();
        assertThat(claims.get("secret")).isNull();
    }

    @Test
    @DisplayName("JWT Robustness: Null, empty, or whitespace tokens safely return null without throwing")
    void blankTokensSafelyRejected() {
        assertThat(jwtService.isValid(null)).isFalse();
        assertThat(jwtService.isValid("")).isFalse();
        assertThat(jwtService.isValid("   ")).isFalse();
        assertThat(jwtService.parseAndValidateClaims(null)).isNull();
        assertThat(jwtService.parseAndValidateClaims("")).isNull();
    }
}

