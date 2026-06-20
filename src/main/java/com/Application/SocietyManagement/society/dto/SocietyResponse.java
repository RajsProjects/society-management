package com.Application.SocietyManagement.society.dto;

import com.Application.SocietyManagement.society.entity.Society;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

import com.Application.SocietyManagement.society.entity.Society;
import com.Application.SocietyManagement.society.enums.SocietyStatus;
import com.Application.SocietyManagement.society.enums.SubscriptionPlan;
import com.Application.SocietyManagement.society.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SocietyResponse {

    private String id;
    private String name;
    private String registrationNumber;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private Integer totalFlats;
    private SocietyStatus status;
    private String joinCode;
    private String documentUrl;
    private SubscriptionPlan plan;
    private SubscriptionStatus subscriptionStatus;
    private Instant trialEndsAt;
    private Instant subscriptionEndsAt;
    private Integer activeResidents;
    private Integer occupiedFlats;
    private Instant createdAt;

    public static SocietyResponse from(Society society) {
        return SocietyResponse.builder()
                .id(society.getId())
                .name(society.getName())
                .registrationNumber(society.getRegistrationNumber())
                .address(society.getAddress())
                .city(society.getCity())
                .state(society.getState())
                .pincode(society.getPincode())
                .totalFlats(society.getTotalFlats())
                .status(society.getStatus())
                .joinCode(society.getJoinCode())
                .documentUrl(society.getDocumentUrl())
                .plan(society.getPlan())
                .subscriptionStatus(society.getSubscriptionStatus())
                .trialEndsAt(society.getTrialEndsAt())
                .subscriptionEndsAt(society.getSubscriptionEndsAt())
                .activeResidents(society.getActiveResidents())
                .occupiedFlats(society.getOccupiedFlats())
                .createdAt(society.getCreatedAt())
                .build();
    }
}