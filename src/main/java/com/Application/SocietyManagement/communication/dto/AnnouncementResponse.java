package com.Application.SocietyManagement.communication.dto;

import com.Application.SocietyManagement.communication.entity.Announcement;
import com.Application.SocietyManagement.communication.enums.AnnouncementType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AnnouncementResponse {

    private String id;
    private String title;
    private String content;
    private AnnouncementType type;
    private String authorId;
    private Instant createdAt;
    private Instant updatedAt;

    public static AnnouncementResponse from(Announcement a) {
        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .type(a.getType())
                .authorId(a.getAuthorId())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
