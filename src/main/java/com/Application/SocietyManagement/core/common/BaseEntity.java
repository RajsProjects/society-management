package com.Application.SocietyManagement.core.common;

import lombok.Getter;
import org.springframework.data.annotation.*;
import java.time.Instant;

@Getter
public abstract class BaseEntity {

    @Id
    private String id;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
