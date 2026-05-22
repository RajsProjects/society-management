package com.Application.SocietyManagement.communication.entity;

import com.Application.SocietyManagement.communication.enums.AnnouncementType;
import com.Application.SocietyManagement.core.common.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "announcements")
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
