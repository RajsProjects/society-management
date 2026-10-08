package com.Application.SocietyManagement.users.service;

import com.Application.SocietyManagement.communication.email.service.EmailService;
import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.users.dto.AcceptInviteRequest;
import com.Application.SocietyManagement.users.dto.AuthResponse;
import com.Application.SocietyManagement.users.dto.InviteRequest;
import com.Application.SocietyManagement.users.entity.InviteToken;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.InviteTokenRepository;
import com.Application.SocietyManagement.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteTokenRepository inviteTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    public Map<String, String> invite(InviteRequest request, User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return invite(request, currentUser.getId(), currentUser.getRole());
    }

    public Map<String, String> invite(InviteRequest request, String invitedById, com.Application.SocietyManagement.users.enums.Roles callerRole) {
        String societyId = TenantContext.getSocietyId();
        if (societyId == null || societyId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No society context");
        }

        if (request.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }

        // Enforce role hierarchy: prevent privilege escalation
        if (callerRole == com.Application.SocietyManagement.users.enums.Roles.ADMIN) {
            if (request.getRole() == com.Application.SocietyManagement.users.enums.Roles.ADMIN
                    || request.getRole() == com.Application.SocietyManagement.users.enums.Roles.SUPER_ADMIN) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Society administrators cannot invite admin or super admin accounts"
                );
            }
        } else if (callerRole == com.Application.SocietyManagement.users.enums.Roles.SUPER_ADMIN) {
            if (request.getRole() == com.Application.SocietyManagement.users.enums.Roles.SUPER_ADMIN) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Super administrators cannot invite additional super administrators"
                );
            }
        } else if (callerRole != null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only administrators can send invitations"
            );
        }

        // check email not already registered
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered");
        }

        // check no pending invite exists
        if (inviteTokenRepository.existsByEmailAndSocietyIdAndUsedFalse(
                request.getEmail(), societyId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invite already sent to this email");
        }

        String token = UUID.randomUUID().toString();

        InviteToken invite = InviteToken.builder()
                .token(token)
                .societyId(societyId)
                .email(request.getEmail())
                .role(request.getRole())
                .flatId(request.getFlatId())
                .expiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                .used(false)
                .invitedBy(invitedById)
                .build();

        inviteTokenRepository.save(invite);

        // send invite email async
        emailService.sendInviteEmail(
                request.getEmail(), token, request.getRole().name());

        log.info("Invite sent to {} for role {} by {}", request.getEmail(),
                request.getRole(), invitedById);

        return Map.of(
                "message", "Invite sent to " + request.getEmail(),
                "expiresIn", "48 hours"
        );
    }

    public Map<String, String> invite(InviteRequest request, String invitedBy) {
        return invite(request, invitedBy, com.Application.SocietyManagement.users.enums.Roles.SUPER_ADMIN);
    }

    public AuthResponse acceptInvite(AcceptInviteRequest request) {
        InviteToken invite = inviteTokenRepository
                .findByToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invalid invite token"));

        if (invite.isUsed()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Invite already used");
        }

        if (invite.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(
                    HttpStatus.GONE, "Invite token has expired");
        }

        // create the user
        User user = User.builder()
                .email(invite.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(invite.getRole())
                .status(Status.ACTIVE)
                .societyId(invite.getSocietyId())
                .flatId(invite.getFlatId())
                .build();

        User saved = userRepository.save(user);

        // mark invite as used
        invite.setUsed(true);
        inviteTokenRepository.save(invite);

        log.info("Invite accepted by {}", invite.getEmail());

        String token = jwtService.generateToken(saved);
        return AuthResponse.builder()
                .token(token)
                .role(saved.getRole().name())
                .message("Account created successfully")
                .build();
    }

    // cleanup expired tokens every hour
    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpiredTokens() {
        inviteTokenRepository.deleteByExpiresAtBefore(Instant.now());
        log.info("Cleaned up expired invite tokens");
    }
}