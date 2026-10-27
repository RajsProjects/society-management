package com.Application.SocietyManagement.core.security;

import com.Application.SocietyManagement.users.dto.AuthResponse;
import com.Application.SocietyManagement.users.dto.LoginRequest;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.PlatformRole;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import com.Application.SocietyManagement.users.service.AuthService;
import com.Application.SocietyManagement.users.service.CustomUserDetailsService;
import com.Application.SocietyManagement.users.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Security: Authentication & Account Security Test Suite")
class AuthenticationSecurityTest {

    private static final String JWT_SECRET = "very-secret-test-key-for-jwt-signing-minimum-32-bytes-long";
    private static final long JWT_EXPIRATION = 3600000L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtService jwtService;
    private AuthService authService;
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private User activeUser;
    private User pendingUser;
    private User inactiveUser;
    private User blockedUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "SECRET", JWT_SECRET);
        ReflectionTestUtils.setField(jwtService, "EXPIRATION", JWT_EXPIRATION);
        jwtService.validateConfiguration();

        authService = new AuthService(userRepository, passwordEncoder, jwtService, null, null);
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, userDetailsService);

        activeUser = User.builder()
                .email("active@test.com")
                .passwordHash("hashed-password")
                .role(Roles.RESIDENT)
                .platformRole(PlatformRole.SOCIETY_USER)
                .status(Status.ACTIVE)
                .societyId("soc-1")
                .build();
        activeUser.setId("user-active-1");

        pendingUser = User.builder()
                .email("pending@test.com")
                .passwordHash("hashed-password")
                .role(Roles.RESIDENT)
                .platformRole(PlatformRole.SOCIETY_USER)
                .status(Status.PENDING)
                .societyId("soc-1")
                .build();
        pendingUser.setId("user-pending-1");

        inactiveUser = User.builder()
                .email("inactive@test.com")
                .passwordHash("hashed-password")
                .role(Roles.RESIDENT)
                .platformRole(PlatformRole.SOCIETY_USER)
                .status(Status.INACTIVE)
                .societyId("soc-1")
                .build();
        inactiveUser.setId("user-inactive-1");

        blockedUser = User.builder()
                .email("blocked@test.com")
                .passwordHash("hashed-password")
                .role(Roles.RESIDENT)
                .platformRole(PlatformRole.SOCIETY_USER)
                .status(Status.BLOCKED)
                .societyId("soc-1")
                .build();
        blockedUser.setId("user-blocked-1");
    }

    @Test
    @DisplayName("Authentication Entry Point returns standardized 401 JSON when token is missing")
    void unauthenticatedRequest_returnsStandardized401() throws Exception {
        com.Application.SocietyManagement.core.config.SecurityConfig config =
                new com.Application.SocietyManagement.core.config.SecurityConfig(null, null, null, null);
        org.springframework.security.web.AuthenticationEntryPoint entryPoint = config.authenticationEntryPoint();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("Full authentication is required"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        assertThat(response.getContentAsString()).contains("\"status\":401");
        assertThat(response.getContentAsString()).contains("\"error\":\"Unauthorized\"");
        assertThat(response.getContentAsString()).contains("/api/v1/users/me");
    }

    @Test
    @DisplayName("Malformed JWT token string is safely rejected without server exception")
    void malformedJwt_isSafelyRejected() {
        assertThat(jwtService.isValid("not-a-valid-jwt-token")).isFalse();
        assertThat(jwtService.parseAndValidateClaims("invalid.token.payload")).isNull();
    }

    @Test
    @DisplayName("Expired JWT token is rejected by parser")
    void expiredJwt_isRejected() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("active@test.com")
                .issuedAt(new Date(System.currentTimeMillis() - 100000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key)
                .compact();

        assertThat(jwtService.isTokenExpired(expiredToken)).isTrue();
        assertThat(jwtService.isValid(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Tampered JWT payload/signature is rejected")
    void tamperedJwt_isRejected() {
        String token = jwtService.generateToken(activeUser);
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThat(jwtService.isValid(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("JWT signed with an untrusted different key is rejected")
    void tokenSignedWithDifferentKey_isRejected() {
        String rogueSecret = "rogue-secret-key-that-does-not-match-application-secret-32b";
        SecretKey rogueKey = Keys.hmacShaKeyFor(rogueSecret.getBytes(StandardCharsets.UTF_8));

        String rogueToken = Jwts.builder()
                .subject("active@test.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(rogueKey)
                .compact();

        assertThat(jwtService.isValid(rogueToken)).isFalse();
    }

    private LoginRequest createLoginRequest(String email, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmail(email);
        req.setPassword(password);
        return req;
    }

    @Test
    @DisplayName("Login with invalid password returns 401 UNAUTHORIZED")
    void login_invalidPassword_returns401() {
        when(userRepository.findByEmail("active@test.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("wrong-password", "hashed-password")).thenReturn(false);

        LoginRequest req = createLoginRequest("active@test.com", "wrong-password");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                });
    }

    @Test
    @DisplayName("Login with unregistered email returns 401 UNAUTHORIZED")
    void login_unregisteredEmail_returns401() {
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        LoginRequest req = createLoginRequest("ghost@test.com", "any-password");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("Login with PENDING status account returns 403 FORBIDDEN")
    void login_pendingAccount_returns403() {
        when(userRepository.findByEmail("pending@test.com")).thenReturn(Optional.of(pendingUser));
        when(passwordEncoder.matches("secret123", "hashed-password")).thenReturn(true);

        LoginRequest req = createLoginRequest("pending@test.com", "secret123");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(rse.getReason()).contains("Account pending admin approval");
                });
    }

    @Test
    @DisplayName("Login with INACTIVE account returns 403 FORBIDDEN")
    void login_inactiveAccount_returns403() {
        when(userRepository.findByEmail("inactive@test.com")).thenReturn(Optional.of(inactiveUser));
        when(passwordEncoder.matches("secret123", "hashed-password")).thenReturn(true);

        LoginRequest req = createLoginRequest("inactive@test.com", "secret123");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    @DisplayName("Login with BLOCKED account returns 403 FORBIDDEN")
    void login_blockedAccount_returns403() {
        when(userRepository.findByEmail("blocked@test.com")).thenReturn(Optional.of(blockedUser));
        when(passwordEncoder.matches("secret123", "hashed-password")).thenReturn(true);

        LoginRequest req = createLoginRequest("blocked@test.com", "secret123");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(rse.getReason()).contains("blocked");
                });
    }

    @Test
    @DisplayName("JwtAuthenticationFilter immediately rejects deactivated/disabled account with 403")
    void jwtFilter_deactivatedAccount_returns403() throws Exception {
        String token = jwtService.generateToken(inactiveUser);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userDetailsService.loadUserByUsername("inactive@test.com")).thenReturn(inactiveUser);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Account is inactive, pending approval, or blocked");
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("JwtAuthenticationFilter allows requests without Authorization header to reach downstream security checks")
    void jwtFilter_noHeader_passesToFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}

