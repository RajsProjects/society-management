package com.Application.SocietyManagement.core.config;

import com.Application.SocietyManagement.society.entity.Society;
import com.Application.SocietyManagement.society.enums.SocietyStatus;
import com.Application.SocietyManagement.society.enums.SubscriptionStatus;
import com.Application.SocietyManagement.society.repository.SocietyRepository;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.PlatformRole;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminSeeder {

    private final UserRepository userRepository;
    private final SocietyRepository societyRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner seedAdmin() {
        return args -> {
            if (userRepository.existsByEmail("admin@society.com")) {
                log.info("Platform admin already exists, skipping seed");
                return;
            }

            Society society = Society.builder()
                    .name("CivicLink Platform")
                    .registrationNumber("PLATFORM-001")
                    .address("123 Main Street")
                    .city("Jaipur")
                    .state("Rajasthan")
                    .pincode("302001")
                    .totalFlats(0)
                    .status(SocietyStatus.ACTIVE)
                    .subscriptionStatus(SubscriptionStatus.ACTIVE)
                    .subscriptionEndsAt(Instant.now().plus(3650, ChronoUnit.DAYS))
                    .societyCode("PLATFORM")
                    .adminEmail("admin@society.com")
                    .build();
            Society saved = societyRepository.save(society);
            log.info("Platform society created: {}", saved.getId());

            User admin = User.builder()
                    .email("admin@society.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .firstName("Platform")
                    .lastName("Admin")
                    .role(Roles.SUPER_ADMIN)
                    .platformRole(PlatformRole.PLATFORM_ADMIN)
                    .status(Status.ACTIVE)
                    .societyId(saved.getId())
                    .build();
            userRepository.save(admin);
            log.info("Platform admin created: admin@society.com / admin123");
        };
    }
}