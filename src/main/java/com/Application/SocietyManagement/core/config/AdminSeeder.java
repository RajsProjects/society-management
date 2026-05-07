package com.Application.SocietyManagement.core.config;

import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        boolean adminExists = userRepository.existsByRole(Roles.ADMIN);

        if (!adminExists) {

            String email = System.getenv("ADMIN_EMAIL");
            String password = System.getenv("ADMIN_PASSWORD");

            if (email == null || password == null) {
                throw new RuntimeException("ADMIN_EMAIL or ADMIN_PASSWORD not set in environment");
            }

            User admin = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .role(Roles.ADMIN)
                    .status(Status.ACTIVE)
                    .build();

            userRepository.save(admin);

            System.out.println("✅ Default ADMIN created");
        }
    }
}
