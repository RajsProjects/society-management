package com.Application.SocietyManagement.flat.dto;

import com.Application.SocietyManagement.flat.enums.FlatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateFlatRequest {

    @NotBlank(message = "Block is required")
    private String block;

    @NotNull(message = "Floor is required")
    private Integer floor;

    @NotBlank(message = "Flat number is required")
    private String flatNumber;

    private String ownerName;
    private String ownerEmail;
    private String ownerPhone;
    private boolean occupied;
    private FlatType type;
    private Integer areaSqFt;
}