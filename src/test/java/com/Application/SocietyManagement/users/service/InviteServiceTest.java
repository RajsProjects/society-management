package com.Application.SocietyManagement.users.service;

import com.Application.SocietyManagement.communication.email.service.EmailService;
import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.users.dto.AcceptInviteRequest;
import com.Application.SocietyManagement.users.dto.AuthResponse;
import com.Application.SocietyManagement.users.dto.InviteRequest;
import com.Application.SocietyManagement.users.entity.InviteToken;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.InviteTokenRepository;
import com.Application.SocietyManagement.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InviteService")
class InviteServiceTest {

    @Mock private InviteTokenRepository inviteTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;

    @InjectMocks
    private InviteService inviteService;

    private static final String SOCIETY_ID = "society-123";
    private InviteRequest inviteRequest;

    @BeforeEach
    void setUp() {
        TenantContext.setSocietyId(SOCIETY_ID);

        inviteRequest = new InviteRequest();
        inviteRequest.setEmail("newresident@example.com");
        inviteRequest.setRole(Roles.RESIDENT);
        inviteRequest.setFlatId("A-101");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("invite - successfully creates invite and sends email")
    void invite_success() {
        when(userRepository.findByEmail("newresident@example.com")).thenReturn(Optional.empty());
        when(inviteTokenRepository.existsByEmailAndSocietyIdAndUsedFalse("newresident@example.com", SOCIETY_ID))
                .thenReturn(false);

        Map<String, String> response = inviteService.invite(inviteRequest, "admin-user");

        assertThat(response).containsEntry("expiresIn", "48 hours");
        assertThat(response.get("message")).contains("newresident@example.com");
        verify(inviteTokenRepository).save(any(InviteToken.class));
        verify(emailService).sendInviteEmail(eq("newresident@example.com"), anyString(), eq("RESIDENT"));
    }

    @Test
    @DisplayName("invite - throws 409 when email is already registered")
    void invite_emailAlreadyExists() {
        when(userRepository.findByEmail("newresident@example.com"))
                .thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> inviteService.invite(inviteRequest, "admin-user"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Email already registered");

        verify(inviteTokenRepository, never()).save(any());
        verify(emailService, never()).sendInviteEmail(any(), any(), any());
    }

    @Test
    @DisplayName("invite - throws 409 when pending invite already exists")
    void invite_pendingInviteAlreadyExists() {
        when(userRepository.findByEmail("newresident@example.com")).thenReturn(Optional.empty());
        when(inviteTokenRepository.existsByEmailAndSocietyIdAndUsedFalse("newresident@example.com", SOCIETY_ID))
                .thenReturn(true);

        assertThatThrownBy(() -> inviteService.invite(inviteRequest, "admin-user"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invite already sent to this email");

        verify(inviteTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("acceptInvite - successfully accepts invite and creates user")
    void acceptInvite_success() {
        InviteToken token = InviteToken.builder()
                .token("valid-token")
                .email("newresident@example.com")
                .role(Roles.RESIDENT)
                .societyId(SOCIETY_ID)
                .flatId("A-101")
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .used(false)
                .build();

        when(inviteTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("Password123!")).thenReturn("hashed-pwd");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-mock-token");

        AcceptInviteRequest acceptReq = new AcceptInviteRequest();
        acceptReq.setToken("valid-token");
        acceptReq.setPassword("Password123!");
        acceptReq.setFirstName("Alice");
        acceptReq.setLastName("Wonderland");

        AuthResponse authResponse = inviteService.acceptInvite(acceptReq);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.getToken()).isEqualTo("jwt-mock-token");
        assertThat(authResponse.getRole()).isEqualTo("RESIDENT");
        assertThat(token.isUsed()).isTrue();
        verify(userRepository).save(any(User.class));
        verify(inviteTokenRepository).save(token);
    }

    @Test
    @DisplayName("acceptInvite - throws 404 when token is invalid")
    void acceptInvite_tokenNotFound() {
        when(inviteTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        AcceptInviteRequest acceptReq = new AcceptInviteRequest();
        acceptReq.setToken("bad-token");

        assertThatThrownBy(() -> inviteService.acceptInvite(acceptReq))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid invite token");
    }

    @Test
    @DisplayName("acceptInvite - throws 409 when token was already used")
    void acceptInvite_alreadyUsed() {
        InviteToken token = InviteToken.builder()
                .token("used-token")
                .used(true)
                .build();

        when(inviteTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        AcceptInviteRequest acceptReq = new AcceptInviteRequest();
        acceptReq.setToken("used-token");

        assertThatThrownBy(() -> inviteService.acceptInvite(acceptReq))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invite already used");
    }

    @Test
    @DisplayName("acceptInvite - throws 410 when token has expired")
    void acceptInvite_expiredToken() {
        InviteToken token = InviteToken.builder()
                .token("expired-token")
                .used(false)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        when(inviteTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        AcceptInviteRequest acceptReq = new AcceptInviteRequest();
        acceptReq.setToken("expired-token");

        assertThatThrownBy(() -> inviteService.acceptInvite(acceptReq))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invite token has expired");
    }
}
