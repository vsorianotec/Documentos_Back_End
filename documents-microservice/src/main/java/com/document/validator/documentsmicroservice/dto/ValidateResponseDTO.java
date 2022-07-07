package com.document.validator.documentsmicroservice.dto;

import lombok.Data;

import java.util.Date;

@Data
public class ValidateResponseDTO extends GenericResponseDTO{
    private int documentId;
    private Date createdDate;
    private String originalName;
    private String author;
    private String email;
}
