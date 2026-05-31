package com.Application.SocietyManagement.users.entity;

import com.Application.SocietyManagement.core.common.BaseEntity;
import com.Application.SocietyManagement.users.enums.Roles;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "invite_tokens")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteToken extends BaseEntity {

    @Indexed(unique = true)
    private String token;

    private String societyId;
    private String email;
    private Roles role;
    private String flatId;
    private Instant expiresAt;
    private boolean used;
    private String invitedBy;
}