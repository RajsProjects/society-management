package com.Application.SocietyManagement.communication.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberDto {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String role;
    private String platformRole;
    private String flatId;
    private String flatNumber;
    private boolean isOwner;
}
