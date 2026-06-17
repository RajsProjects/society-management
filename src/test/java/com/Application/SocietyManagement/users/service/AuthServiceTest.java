package com.Application.SocietyManagement.users.service;

import com.Application.SocietyManagement.users.dto.AuthResponse;
import com.Application.SocietyManagement.users.dto.LoginRequest;
import com.Application.SocietyManagement.users.dto.SignupRequest;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private User activeUser;
    private User pendingUser;
    private User inactiveUser;
    private User blockedUser;

    @BeforeEach
    void setUp() {
        signupRequest = new SignupRequest();
        signupRequest.setEmail("resident@test.com");
        signupRequest.setPassword("password123");
        signupRequest.setFirstName("John");
        signupRequest.setLastName("Doe");
        signupRequest.setApartmentNumber("A-101");

        loginRequest = new LoginRequest();
        loginRequest.setEmail("resident@test.com");
        loginRequest.setPassword("password123");

        activeUser = User.builder()
                .email("resident@test.com")
                .passwordHash("hashedPassword")
                .firstName("John")
                .lastName("Doe")
                .role(Roles.RESIDENT)
                .status(Status.ACTIVE)
                .build();

        pendingUser = User.builder()
                .email("pending@test.com")
                .passwordHash("hashedPassword")
                .role(Roles.RESIDENT)
                .status(Status.PENDING)
                .build();

        inactiveUser = User.builder()
                .email("inactive@test.com")
                .passwordHash("hashedPassword")
                .role(Roles.RESIDENT)
                .status(Status.INACTIVE)
                .build();

        blockedUser = User.builder()
                .email("blocked@test.com")
                .passwordHash("hashedPassword")
                .role(Roles.RESIDENT)
                .status(Status.BLOCKED)
                .build();
    }

    @Nested
    @DisplayName("signup")
    class Signup {

        @Test
        @DisplayName("success - creates user with PENDING status")
        void signup_success_returnsPendingMessage() {
            when(userRepository.findByEmail(signupRequest.getEmail()))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode(signupRequest.getPassword()))
                    .thenReturn("hashedPassword");
            when(userRepository.save(any(User.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            AuthResponse response = authService.signup(signupRequest);

            assertThat(response.getMessage()).contains("Awaiting admin approval");
            assertThat(response.getToken()).isNull();
            verify(userRepository).save(argThat(user ->
                    user.getStatus() == Status.PENDING &&
                            user.getRole() == Roles.RESIDENT &&
                            user.getEmail().equals("resident@test.com")
            ));
        }

        @Test
        @DisplayName("duplicate email - throws 409 CONFLICT")
        void signup_duplicateEmail_throwsConflict() {
            when(userRepository.findByEmail(signupRequest.getEmail()))
                    .thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> authService.signup(signupRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(409);
                        assertThat(e.getMessage()).contains("Email already registered");
                    });

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("password is hashed - never stored as plain text")
        void signup_passwordIsHashed() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("$2a$bcrypt$hash");
            when(userRepository.save(any(User.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            authService.signup(signupRequest);

            verify(userRepository).save(argThat(user ->
                    !user.getPasswordHash().equals("password123") &&
                            user.getPasswordHash().equals("$2a$bcrypt$hash")
            ));
            verify(passwordEncoder).encode("password123");
        }

        @Test
        @DisplayName("new user - default role is RESIDENT")
        void signup_defaultRole_isResident() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(userRepository.save(any(User.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            authService.signup(signupRequest);

            verify(userRepository).save(argThat(user ->
                    user.getRole() == Roles.RESIDENT));
        }

        @Test
        @DisplayName("signup - saves apartment number correctly")
        void signup_savesApartmentNumber() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(userRepository.save(any(User.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            authService.signup(signupRequest);

            verify(userRepository).save(argThat(user ->
                    user.getApartmentNumber().equals("A-101")));
        }

        @Test
        @DisplayName("signup - saves first and last name correctly")
        void signup_savesFullName() {
            when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
            when(passwordEncoder.encode(any())).thenReturn("hashed");
            when(userRepository.save(any(User.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            authService.signup(signupRequest);

            verify(userRepository).save(argThat(user ->
                    user.getFirstName().equals("John") &&
                            user.getLastName().equals("Doe")));
        }
    }

    @Nested
    @DisplayName("login")
    class Login {

        @Test
        @DisplayName("success - returns JWT token and role")
        void login_success_returnsTokenAndRole() {
            when(userRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches("password123", "hashedPassword"))
                    .thenReturn(true);
            when(jwtService.generateToken(activeUser))
                    .thenReturn("jwt.token.here");

            AuthResponse response = authService.login(loginRequest);

            assertThat(response.getToken()).isEqualTo("jwt.token.here");
            assertThat(response.getRole()).isEqualTo("RESIDENT");
        }

        @Test
        @DisplayName("wrong email - throws 401 UNAUTHORIZED")
        void login_emailNotFound_throwsUnauthorized() {
            when(userRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(401);
                        assertThat(e.getMessage()).contains("Invalid email or password");
                    });
        }

        @Test
        @DisplayName("wrong password - throws 401 UNAUTHORIZED")
        void login_wrongPassword_throwsUnauthorized() {
            when(userRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(any(), any())).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(401);
                    });
        }

        @Test
        @DisplayName("PENDING user - throws 403 FORBIDDEN")
        void login_pendingUser_throwsForbidden() {
            when(userRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(pendingUser));
            when(passwordEncoder.matches(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(403);
                        assertThat(e.getMessage()).contains("pending admin approval");
                    });

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("INACTIVE user - throws 403 FORBIDDEN")
        void login_inactiveUser_throwsForbidden() {
            when(userRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(inactiveUser));
            when(passwordEncoder.matches(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(403);
                        assertThat(e.getMessage()).contains("inactive");
                    });

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("BLOCKED user - throws 403 FORBIDDEN")
        void login_blockedUser_throwsForbidden() {
            when(userRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(blockedUser));
            when(passwordEncoder.matches(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException e = (ResponseStatusException) ex;
                        assertThat(e.getStatusCode().value()).isEqualTo(403);
                    });

            verify(jwtService, never()).generateToken(any());
        }

        @Test
        @DisplayName("success - JWT generated with correct user object")
        void login_success_jwtGeneratedWithCorrectUser() {
            when(userRepository.findByEmail(loginRequest.getEmail()))
                    .thenReturn(Optional.of(activeUser));
            when(passwordEncoder.matches(any(), any())).thenReturn(true);
            when(jwtService.generateToken(activeUser)).thenReturn("token");

            authService.login(loginRequest);

            verify(jwtService).generateToken(activeUser);
        }

        @Test
        @DisplayName("admin login - returns ADMIN role")
        void login_adminUser_returnsAdminRole() {
            User adminUser = User.builder()
                    .email("admin@test.com")
                    .passwordHash("hashedPassword")
                    .role(Roles.ADMIN)
                    .status(Status.ACTIVE)
                    .build();

            loginRequest.setEmail("admin@test.com");
            when(userRepository.findByEmail("admin@test.com"))
                    .thenReturn(Optional.of(adminUser));
            when(passwordEncoder.matches(any(), any())).thenReturn(true);
            when(jwtService.generateToken(adminUser)).thenReturn("admin.token");

            AuthResponse response = authService.login(loginRequest);

            assertThat(response.getRole()).isEqualTo("ADMIN");
            assertThat(response.getToken()).isEqualTo("admin.token");
        }
    }
}