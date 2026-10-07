package com.Application.SocietyManagement.flat.entity;

import com.Application.SocietyManagement.core.common.BaseEntity;
import com.Application.SocietyManagement.flat.enums.FlatType;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "flats")
@CompoundIndexes({
        @CompoundIndex(def = "{'societyId': 1, 'flatNumber': 1}", unique = true),
        @CompoundIndex(def = "{'societyId': 1, 'block': 1, 'flatNumber': 1}", name = "flat_society_block_flat_idx"),
        @CompoundIndex(def = "{'societyId': 1, 'occupied': 1}", name = "flat_society_occupied_idx")
})
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