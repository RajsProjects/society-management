package com.Application.SocietyManagement.Flat.dto;

import com.Application.SocietyManagement.Flat.entity.Flat;
import com.Application.SocietyManagement.Flat.enums.FlatType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FlatResponse {

    private String id;
    private String societyId;
    private String block;
    private Integer floor;
    private String flatNumber;
    private String ownerName;
    private String ownerEmail;
    private String ownerPhone;
    private boolean occupied;
    private FlatType type;
    private Integer areaSqFt;
    private Instant createdAt;

    public static FlatResponse from(Flat flat) {
        return FlatResponse.builder()
                .id(flat.getId())
                .societyId(flat.getSocietyId())
                .block(flat.getBlock())
                .floor(flat.getFloor())
                .flatNumber(flat.getFlatNumber())
                .ownerName(flat.getOwnerName())
                .ownerEmail(flat.getOwnerEmail())
                .ownerPhone(flat.getOwnerPhone())
                .occupied(flat.isOccupied())
                .type(flat.getType())
                .areaSqFt(flat.getAreaSqFt())
                .createdAt(flat.getCreatedAt())
                .build();
    }
}
