package com.Application.SocietyManagement.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequestDto {

    @NotBlank
    private String name;

    @Email
    private String email;

    @NotBlank
    private String password;
}
