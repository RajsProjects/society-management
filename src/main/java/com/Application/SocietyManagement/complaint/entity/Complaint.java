package com.Application.SocietyManagement.complaint.entity;

import com.Application.SocietyManagement.complaint.enums.ComplaintCategory;
import com.Application.SocietyManagement.complaint.enums.ComplaintStatus;
import com.Application.SocietyManagement.core.common.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "complaints")
@CompoundIndexes({
        @CompoundIndex(def = "{'societyId': 1, 'status': 1, 'createdAt': -1}", name = "complaint_society_status_idx"),
        @CompoundIndex(def = "{'societyId': 1, 'category': 1, 'createdAt': -1}", name = "complaint_society_cat_idx"),
        @CompoundIndex(def = "{'societyId': 1, 'status': 1, 'category': 1, 'createdAt': -1}", name = "complaint_society_status_cat_idx"),
        @CompoundIndex(def = "{'societyId': 1, 'residentId': 1, 'status': 1, 'createdAt': -1}", name = "complaint_society_resident_status_idx"),
        @CompoundIndex(def = "{'societyId': 1, 'createdAt': -1}", name = "complaint_society_created_idx")
})
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