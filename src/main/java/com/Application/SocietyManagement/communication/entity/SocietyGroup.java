package com.Application.SocietyManagement.communication.entity;

import com.Application.SocietyManagement.core.common.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.HashSet;
import java.util.Set;

/**
 * SocietyGroup represents the single official community group per society.
 * Exactly 1 group exists per registered society, automatically created on society registration.
 * All residents and society admins are automatically added upon onboarding.
 * Multi-group membership is strictly restricted to PLATFORM_ADMIN users.
 */
@Document(collection = "society_groups")
@CompoundIndexes({
        @CompoundIndex(def = "{'societyId': 1}", name = "society_group_unique_idx", unique = true)
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocietyGroup extends BaseEntity {

    private String societyId;

    private String name;

    private String description;

    @Builder.Default
    private Set<String> memberUserIds = new HashSet<>();

    @Builder.Default
    private boolean createdAutomatically = true;
}
