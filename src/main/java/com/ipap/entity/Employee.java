package com.ipap.entity;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Date;
import java.util.UUID;

@ToString
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document
public class Employee {
    @MongoId
    private String id;
    private String firstname;
    private String lastname;
    private String email;
    private String position;
    private Float salary;
    private Date createdAt;
    private Date modifiedAt;
    private Boolean isActiveAccount;
}


