package com.Application.SocietyManagement.society.dto;

import com.Application.SocietyManagement.society.entity.Society;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SocietyResponse {

    private String id;
    private String societyCode;
    private String name;
    private String address;
    private String adminEmail;
    private Instant createdAt;
    private String message;

    public static SocietyResponse from(Society society) {
        return SocietyResponse.builder()
                .id(society.getId())
                .societyCode(society.getSocietyCode())
                .name(society.getName())
                .address(society.getAddress())
                .adminEmail(society.getAdminEmail())
                .createdAt(society.getCreatedAt())
                .build();
    }
}