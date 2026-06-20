package com.Application.SocietyManagement.society.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterSocietyRequest {

    @NotBlank(message = "Society name is required")
    private String name;

    @NotBlank(message = "Registration number is required")
    private String registrationNumber;

    @NotBlank(message = "Address is required")
    private String address;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$",
            message = "Invalid pincode")
    private String pincode;

    @NotNull(message = "Total flats is required")
    @Min(value = 1, message = "Must have at least 1 flat")
    @Max(value = 5000, message = "Contact us for societies above 5000 flats")
    private Integer totalFlats;

    // Super admin details
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$",
            message = "Invalid Indian mobile number")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
