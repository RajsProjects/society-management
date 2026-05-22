package com.Application.SocietyManagement.society.service;

import com.Application.SocietyManagement.society.dto.SocietyRegistrationRequest;
import com.Application.SocietyManagement.society.dto.SocietyResponse;
import com.Application.SocietyManagement.society.dto.UpdateSocietyRequest;
import com.Application.SocietyManagement.society.entity.Society;
import com.Application.SocietyManagement.society.repository.SocietyRepository;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocietyService {

    private final SocietyRepository societyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SocietyResponse register(SocietyRegistrationRequest request) {

        // check society code uniqueness
        if (societyRepository.existsBySocietyCode(
                request.getSocietyCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Society code already exists: " + request.getSocietyCode());
        }

        // check admin email uniqueness
        if (userRepository.findByEmail(request.getAdminEmail()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email already registered: " + request.getAdminEmail());
        }

        // create society
        Society society = Society.builder()
                .name(request.getSocietyName())
                .societyCode(request.getSocietyCode()
                        .toUpperCase())
                .address(request.getAddress())
                .adminEmail(request.getAdminEmail())
                .build();

        Society saved = societyRepository.save(society);

        // create admin user for this society
        User admin = User.builder()
                .email(request.getAdminEmail())
                .passwordHash(passwordEncoder.encode(
                        request.getAdminPassword()))
                .firstName(request.getAdminFirstName())
                .lastName(request.getAdminLastName())
                .role(Roles.ADMIN)
                .status(Status.ACTIVE)
                .societyId(saved.getId())
                .build();

        userRepository.save(admin);

        log.info("New society registered: {} with admin: {}",
                saved.getSocietyCode(), request.getAdminEmail());

        return SocietyResponse.builder()
                .id(saved.getId())
                .societyCode(saved.getSocietyCode())
                .name(saved.getName())
                .address(saved.getAddress())
                .adminEmail(saved.getAdminEmail())
                .createdAt(saved.getCreatedAt())
                .message("Society registered successfully. " +
                        "Admin account created and ready to login.")
                .build();
    }

    public SocietyResponse getBySocietyId(String societyId) {
        Society society = societyRepository.findById(societyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Society not found"));
        return SocietyResponse.from(society);
    }

    public SocietyResponse update(String societyId,
                                  UpdateSocietyRequest request) {
        Society society = societyRepository.findById(societyId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Society not found"));

        society.setName(request.getName());
        society.setAddress(request.getAddress());

        return SocietyResponse.from(societyRepository.save(society));
    }
}