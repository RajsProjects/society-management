package com.Application.SocietyManagement.complaint.dto;

import com.Application.SocietyManagement.complaint.entity.Complaint;
import com.Application.SocietyManagement.complaint.enums.ComplaintCategory;
import com.Application.SocietyManagement.complaint.enums.ComplaintStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ComplaintResponse {

    private String id;
    private String societyId;
    private String residentId;
    private String residentName;
    private String apartmentNumber;
    private String title;
    private String description;
    private ComplaintCategory category;
    private ComplaintStatus status;
    private String adminNote;
    private String resolvedBy;
    private Instant createdAt;
    private Instant updatedAt;

    public static ComplaintResponse from(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .societyId(complaint.getSocietyId())
                .residentId(complaint.getResidentId())
                .residentName(complaint.getResidentName())
                .apartmentNumber(complaint.getApartmentNumber())
                .title(complaint.getTitle())
                .description(complaint.getDescription())
                .category(complaint.getCategory())
                .status(complaint.getStatus())
                .adminNote(complaint.getAdminNote())
                .resolvedBy(complaint.getResolvedBy())
                .createdAt(complaint.getCreatedAt())
                .updatedAt(complaint.getUpdatedAt())
                .build();
    }
}