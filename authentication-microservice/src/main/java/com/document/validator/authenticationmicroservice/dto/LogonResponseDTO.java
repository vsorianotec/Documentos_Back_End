package com.document.validator.authenticationmicroservice.dto;

import com.document.validator.authenticationmicroservice.entity.User;
import lombok.Data;

@Data
public class LogonResponseDTO extends GenericResponseDTO{
    private User user;
}