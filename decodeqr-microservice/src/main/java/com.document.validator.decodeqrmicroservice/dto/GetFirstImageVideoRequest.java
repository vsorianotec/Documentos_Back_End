package com.document.validator.decodeqrmicroservice.dto;

import lombok.Data;

@Data
public class GetFirstImageVideoRequest {
    String inputVideoPath;
    String outputImagePath;
}
