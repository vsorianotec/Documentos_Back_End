package com.document.validator.authenticationmicroservice.dto;

import lombok.Data;

@Data
public class LogonRequestDTO {
    private String email;
    private String password;
}