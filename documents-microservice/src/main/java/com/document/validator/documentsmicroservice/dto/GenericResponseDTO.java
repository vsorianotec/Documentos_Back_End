package com.document.validator.documentsmicroservice.dto;

import lombok.Data;

@Data
public class GenericResponseDTO {
    private int status;
    private String codeError;
    private String msgError;
}