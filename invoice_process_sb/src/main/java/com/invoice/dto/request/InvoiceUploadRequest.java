package com.invoice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class InvoiceUploadRequest {

    @NotNull(message = "File is required")
    private MultipartFile file;

    private String notes;
}

