package com.Application.SocietyManagement.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocietyGroupResponse {
    private String id;
    private String societyId;
    private String name;
    private String description;
    private int memberCount;
    private List<GroupMemberDto> members;
    private boolean createdAutomatically;
    private Instant createdAt;
    private Instant updatedAt;
}
