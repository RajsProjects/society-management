package com.Application.SocietyManagement.core.config;

import com.Application.SocietyManagement.society.entity.Society;
import com.Application.SocietyManagement.society.repository.SocietyRepository;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

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
            if (societyRepository.count() == 0) {
                Society society = Society.builder()
                        .name("Default Society")
                        .societyCode("DEFAULT")
                        .address("123 Main Street")
                        .adminEmail("admin@society.com")
                        .build();
                Society saved = societyRepository.save(society);
                log.info("Default society created: {}", saved.getId());

                if (!userRepository.existsByRole(Roles.ADMIN)) {
                    User admin = User.builder()
                            .email("admin@society.com")
                            .passwordHash(passwordEncoder
                                    .encode("admin123"))
                            .firstName("Super")
                            .lastName("Admin")
                            .role(Roles.ADMIN)
                            .status(Status.ACTIVE)
                            .societyId(saved.getId())
                            .build();
                    userRepository.save(admin);
                    log.info("Default admin created: admin@society.com");
                }
            }
        };
    }
}