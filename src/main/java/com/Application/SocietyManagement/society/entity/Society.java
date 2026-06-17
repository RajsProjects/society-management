package com.Application.SocietyManagement.society.entity;

import com.Application.SocietyManagement.core.common.BaseEntity;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "societies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Society extends BaseEntity {

    private String name;

    @Indexed(unique = true)
    private String societyCode;

    private String address;
    private String adminEmail;
}