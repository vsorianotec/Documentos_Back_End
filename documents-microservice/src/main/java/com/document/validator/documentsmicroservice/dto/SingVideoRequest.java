package com.document.validator.documentsmicroservice.dto;

import lombok.Data;

@Data
public class SingVideoRequest {
    String inputVideoPath;
    String outputVideoPath;
    String qrCodePath;
}
