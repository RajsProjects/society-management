package com.Application.SocietyManagement.society.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SocietyRegistrationRequest {

    @NotBlank(message = "Society name is required")
    private String societyName;

    @NotBlank(message = "Society code is required")
    @Size(min = 3, max = 10, message = "Society code must be between 3 and 10 characters")
    private String societyCode;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "Admin email is required")
    @Email(message = "Invalid email format")
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String adminPassword;

    @NotBlank(message = "Admin first name is required")
    private String adminFirstName;

    @NotBlank(message = "Admin last name is required")
    private String adminLastName;
}