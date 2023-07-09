package com.document.validator.decodeqrmicroservice.dto;

import lombok.Data;

@Data
public class SingVideoRequest {
    String inputVideoPath;
    String outputVideoPath;
    String qrCodePath;
}
