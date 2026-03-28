package com.ai.vidya.modules.school.dto.request;

import com.ai.vidya.common.enums.DocumentType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Step 5 — Document metadata after file upload to S3/CDN */
@Data
public class DocumentUploadRequest {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @Size(max = 255)
    private String documentName;

    /** S3 key / CDN URL returned after the file upload */
    @NotNull(message = "File URL is required")
    @Size(max = 512)
    private String fileUrl;

    @Size(max = 100)
    private String mimeType;

    private Long fileSizeBytes;

    @Size(max = 255)
    private String originalFileName;
}