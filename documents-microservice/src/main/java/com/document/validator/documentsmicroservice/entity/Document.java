package com.document.validator.documentsmicroservice.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "documento")
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String fileName;
    private String description;
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdDate;
    private int createdBy;
    private String hashOriginalDocument;
    private String hashSignedDocument;
}
