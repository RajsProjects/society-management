package com.Application.SocietyManagement.core.common;

import lombok.Getter;
import org.springframework.data.annotation.*;
import org.springframework.security.core.GrantedAuthority;

import java.time.Instant;
import java.util.Collection;

@Getter
public abstract class BaseEntity {

    @Id
    private String id;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
