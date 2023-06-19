package com.document.validator.documentsmicroservice.dto;

import com.document.validator.documentsmicroservice.entity.Document;
import lombok.Data;

@Data
public class CompareMinisResponseDTO extends GenericResponseDTO{
    private double matchPercentage;
    private Document document;
    private String emailAuthor;
    private String nameAuthor;
}
