package com.Application.SocietyManagement.communication.entity;

import com.Application.SocietyManagement.communication.enums.AnnouncementType;
import com.Application.SocietyManagement.core.common.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "announcements")
@CompoundIndexes({
        @CompoundIndex(def = "{'societyId': 1, 'createdAt': -1}", name = "announcement_society_created_idx"),
        @CompoundIndex(def = "{'societyId': 1, 'type': 1, 'createdAt': -1}", name = "announcement_society_type_created_idx")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement extends BaseEntity {

    private String title;
    private String content;
    private AnnouncementType type;
    private String authorId;
    private String societyId;
}
