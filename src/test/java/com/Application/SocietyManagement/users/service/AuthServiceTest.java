package com.Application.SocietyManagement.users.service;

import com.Application.SocietyManagement.users.dto.AuthResponse;
import com.Application.SocietyManagement.users.dto.LoginRequest;
import com.Application.SocietyManagement.users.dto.SignupRequest;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private SignupRequest signupRequest;
    private LoginRequest loginRequest;
    private User activeUser;

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
    }

    // ── Signup tests ──

    @Test
    void signup_success_returnsPendingMessage() {
        when(userRepository.findByEmail(signupRequest.getEmail()))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(signupRequest.getPassword()))
                .thenReturn("hashedPassword");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        AuthResponse response = authService.signup(signupRequest);

        assertThat(response.getMessage())
                .contains("Awaiting admin approval");
        assertThat(response.getToken()).isNull();

        verify(userRepository).save(argThat(user ->
                user.getStatus() == Status.PENDING &&
                        user.getRole() == Roles.RESIDENT &&
                        user.getEmail().equals("resident@test.com")
        ));
    }

    @Test
    void signup_duplicateEmail_throwsConflict() {
        when(userRepository.findByEmail(signupRequest.getEmail()))
                .thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.signup(signupRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void signup_passwordIsHashed_notStoredAsPlainText() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.signup(signupRequest);

        verify(userRepository).save(argThat(user ->
                !user.getPasswordHash().equals("password123") &&
                        user.getPasswordHash().equals("hashedPassword")
        ));
    }

    // ── Login tests ──

    @Test
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
    void login_emailNotFound_throwsUnauthorized() {
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_pendingUser_throwsForbidden() {
        activeUser.setStatus(Status.PENDING);
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("pending admin approval");
    }

    @Test
    void login_inactiveUser_throwsForbidden() {
        activeUser.setStatus(Status.INACTIVE);
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void login_success_jwtGeneratedWithCorrectUser() {
        when(userRepository.findByEmail(loginRequest.getEmail()))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);
        when(jwtService.generateToken(activeUser)).thenReturn("token");

        authService.login(loginRequest);

        verify(jwtService).generateToken(activeUser);
    }
}
