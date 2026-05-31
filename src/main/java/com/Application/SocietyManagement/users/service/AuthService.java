package com.Application.SocietyManagement.users.service;

import com.Application.SocietyManagement.users.dto.AuthResponse;
import com.Application.SocietyManagement.users.dto.LoginRequest;
import com.Application.SocietyManagement.users.dto.SignupRequest;
import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import com.Application.SocietyManagement.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse signup(SignupRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Email already registered");
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .apartmentNumber(request.getApartmentNumber())
                .role(Roles.RESIDENT)
                .status(Status.PENDING)
                .build();

        userRepository.save(user);

        return AuthResponse.builder()
                .message("Registration successful. Awaiting admin approval.")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        if (user.getStatus() == Status.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Account pending admin approval");
        }

        if (user.getStatus() == Status.INACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Account is inactive");
        }

        if (user.getStatus() == Status.BLOCKED) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Account has been blocked");
        }

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .role(user.getRole().name())
                .build();
    }
}