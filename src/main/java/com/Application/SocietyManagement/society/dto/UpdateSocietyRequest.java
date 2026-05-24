package com.Application.SocietyManagement.society.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSocietyRequest {

    @NotBlank(message = "Society name is required")
    private String name;

    @NotBlank(message = "Address is required")
    private String address;
}