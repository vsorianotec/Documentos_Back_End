package com.example.demo.dto;

import lombok.Data;

@Data
public class LogonRequestDTO {
    private String email;
    private String password;
}