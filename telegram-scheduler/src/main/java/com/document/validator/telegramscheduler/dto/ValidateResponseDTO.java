package com.document.validator.telegramscheduler.dto;

import lombok.Data;
import java.util.Date;

@Data
public class ValidateResponseDTO extends GenericResponseDTO{
    private String fileName;
    private int documentId;
    private Date createdDate;
    private String originalName;
    private String author;
    private String email;
}
