package com.Application.SocietyManagement.complaint.entity;

import com.Application.SocietyManagement.complaint.enums.ComplaintCategory;
import com.Application.SocietyManagement.complaint.enums.ComplaintStatus;
import com.Application.SocietyManagement.core.common.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "complaints")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Complaint extends BaseEntity {

    @Indexed
    private String societyId;

    @Indexed
    private String residentId;

    private String residentName;
    private String apartmentNumber;
    private String title;
    private String description;
    private ComplaintCategory category;

    @Builder.Default
    private ComplaintStatus status = ComplaintStatus.OPEN;

    private String adminNote;
    private String resolvedBy;
}