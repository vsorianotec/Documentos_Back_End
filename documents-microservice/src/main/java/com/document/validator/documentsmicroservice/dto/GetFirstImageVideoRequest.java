package com.document.validator.documentsmicroservice.dto;

import lombok.Data;

@Data
public class GetFirstImageVideoRequest {
    String inputVideoPath;
    String outputImagePath;
}
