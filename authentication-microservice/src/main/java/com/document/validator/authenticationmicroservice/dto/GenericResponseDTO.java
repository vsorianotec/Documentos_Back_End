package com.document.validator.authenticationmicroservice.dto;

import lombok.Data;

@Data
public class GenericResponseDTO {
    private int status;
    private String codeError;
    private String msgError;
}