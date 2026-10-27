package com.Application.SocietyManagement.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateResidentRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    private String flatId;

    private Boolean isOwner;

    @Pattern(regexp = "^$|^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$", message = "Password must be at least 8 characters long and contain uppercase, lowercase, and number")
    private String newPassword;

    private String notes;
}
