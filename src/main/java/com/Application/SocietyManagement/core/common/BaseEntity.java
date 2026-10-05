package com.Application.SocietyManagement.core.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.*;
import org.springframework.security.core.GrantedAuthority;

import java.time.Instant;
import java.util.Collection;

@Getter
@Setter
public abstract class BaseEntity {

    @Id
    private String id;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
