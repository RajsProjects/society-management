package com.Application.SocietyManagement.users.dto;

import com.Application.SocietyManagement.users.entity.User;
import com.Application.SocietyManagement.users.enums.Roles;
import com.Application.SocietyManagement.users.enums.Status;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class UserSummarydto {

    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String apartmentNumber;
    private Roles role;
    private Status status;
    private Instant createdAt;

    public static UserSummarydto from(User user) {
        return UserSummarydto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .apartmentNumber(user.getApartmentNumber())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
