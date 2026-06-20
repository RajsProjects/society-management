package com.Application.SocietyManagement.society.service;

import com.Application.SocietyManagement.core.service.S3Service;
import com.Application.SocietyManagement.core.tenant.TenantContext;
import com.Application.SocietyManagement.core.util.JoinCodeGenerator;
import com.Application.SocietyManagement.society.dto.RegisterSocietyRequest;
import com.Application.SocietyManagement.society.dto.SocietyResponse;
import com.Application.SocietyManagement.society.dto.VerifySocietyRequest;
import com.Application.SocietyManagement.society.entity.Society;
import com.Application.SocietyManagement.society.enums.SocietyStatus;
import com.Application.SocietyManagement.society.enums.SubscriptionPlan;
import com.Application.SocietyManagement.society.enums.SubscriptionStatus;
import com.Application.SocietyManagement.society.repository.SocietyRepository;
import com.Application.SocietyManagement.users.dto.PagedResponse;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.PlatformRole;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import com.Application.SocietyManagement.users.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocietyService {

    private final SocietyRepository societyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final S3Service s3Service;
    private final JoinCodeGenerator joinCodeGenerator;

    @Value("${platform.trial-days:14}")
    private int trialDays;

    // ── Public: Society Registration ──

    public Map<String, String> register(
            RegisterSocietyRequest request,
            MultipartFile document) throws IOException {

        // validations
        if (societyRepository.existsByRegistrationNumber(
                request.getRegistrationNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Society with this registration number already exists");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered");
        }

        // upload document to S3
        String documentKey = null;
        if (document != null && !document.isEmpty()) {
            documentKey = s3Service.uploadFile(
                    document.getBytes(),
                    document.getContentType(),
                    "society-documents"
            );
        }

        // generate unique join code
        String joinCode = generateUniqueJoinCode();

        // create society
        Society society = Society.builder()
                .name(request.getName())
                .registrationNumber(request.getRegistrationNumber())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .totalFlats(request.getTotalFlats())
                .adminEmail(request.getEmail())
                .status(SocietyStatus.PENDING_VERIFICATION)
                .documentUrl(documentKey)
                .joinCode(joinCode)
                .societyCode(joinCode)
                .plan(SubscriptionPlan.TRIAL)
                .subscriptionStatus(SubscriptionStatus.TRIAL)
                .build();

        Society saved = societyRepository.save(society);

        // create SUPER_ADMIN user
        User superAdmin = User.builder()
                .societyId(saved.getId())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role(Roles.SUPER_ADMIN)
                .platformRole(PlatformRole.SOCIETY_USER)
                .status(Status.PENDING) // active after society verified
                .build();

        User savedAdmin = userRepository.save(superAdmin);

        // link superAdmin to society
        saved.setSuperAdminId(savedAdmin.getId());
        societyRepository.save(saved);

        log.info("Society registered: {} by {}",
                saved.getName(), request.getEmail());

        return Map.of(
                "message", "Society registered successfully. " +
                        "Verification takes 1-2 business days.",
                "societyId", saved.getId(),
                "status", "PENDING_VERIFICATION"
        );
    }

    // ── Platform Admin: Verify Society ──

    public SocietyResponse verify(String societyId,
                                  VerifySocietyRequest request,
                                  String platformAdminId) {

        Society society = findById(societyId);

        if (society.getStatus() != SocietyStatus.PENDING_VERIFICATION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Society is not pending verification");
        }

        if (request.getStatus() == SocietyStatus.REJECTED) {
            if (request.getRejectionReason() == null ||
                    request.getRejectionReason().isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Rejection reason is required");
            }
            society.setStatus(SocietyStatus.REJECTED);
            society.setRejectionReason(request.getRejectionReason());
            log.info("Society rejected: {}", societyId);

        } else if (request.getStatus() == SocietyStatus.ACTIVE) {
            society.setStatus(SocietyStatus.ACTIVE);
            society.setVerifiedAt(Instant.now());
            society.setVerifiedBy(platformAdminId);
            society.setTrialEndsAt(
                    Instant.now().plus(trialDays, ChronoUnit.DAYS));

            // activate the SUPER_ADMIN
            userRepository.findById(society.getSuperAdminId())
                    .ifPresent(admin -> {
                        admin.setStatus(Status.ACTIVE);
                        userRepository.save(admin);
                    });

            log.info("Society approved: {} trial ends: {}",
                    societyId, society.getTrialEndsAt());
        }

        return SocietyResponse.from(societyRepository.save(society));
    }

    // ── Public: Join via code ──

    public SocietyResponse getByJoinCode(String joinCode) {
        Society society = societyRepository.findByJoinCode(joinCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invalid join code"));

        if (society.getStatus() != SocietyStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This society is not active");
        }

        return SocietyResponse.from(society);
    }

    // ── Platform Admin: List all societies ──

    public PagedResponse<SocietyResponse> listAll(
            SocietyStatus status, int page, int size) {

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Society> result = status != null
                ? societyRepository.findByStatus(status, pageable)
                : societyRepository.findAll(pageable);

        return PagedResponse.<SocietyResponse>builder()
                .content(result.getContent().stream()
                        .map(SocietyResponse::from)
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .build();
    }

    // ── Authenticated: Get own society ──

    public SocietyResponse getMySociety() {
        String societyId = TenantContext.getSocietyId();
        return SocietyResponse.from(findById(societyId));
    }

    // ── Document URL (presigned) ──

    public String getDocumentUrl(String societyId) {
        Society society = findById(societyId);
        if (society.getDocumentUrl() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "No document uploaded");
        }
        return s3Service.generatePresignedUrl(society.getDocumentUrl());
    }

    private Society findById(String societyId) {
        return societyRepository.findById(societyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Society not found"));
    }

    private String generateUniqueJoinCode() {
        String code;
        do {
            code = joinCodeGenerator.generate();
        } while (societyRepository.findByJoinCode(code).isPresent());
        return code;
    }
}