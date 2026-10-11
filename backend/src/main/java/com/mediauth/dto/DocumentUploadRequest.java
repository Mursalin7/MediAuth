package com.mediauth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentUploadRequest {

    @NotBlank(message = "Document type is required")
    private String docType;

    @NotNull(message = "File URL is required")
    private String fileUrl;
}
