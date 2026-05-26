package com.Application.SocietyManagement.Flat.entity;

import com.Application.SocietyManagement.Flat.enums.FlatType;
import com.Application.SocietyManagement.core.common.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "flats")
@CompoundIndex(def = "{'societyId': 1, 'flatNumber': 1}", unique = true)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Flat extends BaseEntity {

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
}
