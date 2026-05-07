package com.Application.SocietyManagement.issue.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreatorDto {
    private String id;
    private String firstName;
    private String lastName;
}
