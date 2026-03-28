package com.ai.vidya.modules.school.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Returned after processing a bulk CSV/Excel upload */
@Data
@Builder
public class BulkUploadResult {

    private int totalRows;
    private int successCount;
    private int failureCount;
    private int skippedCount;

    @Builder.Default
    private List<RowResult> results = new ArrayList<>();

    @Data
    @Builder
    public static class RowResult {

        private int     rowNumber;
        private boolean success;
        private UUID    schoolId;
        private String  schoolName;

        /** Temporary password — only present when success = true */
        private String  adminEmail;
        private String  temporaryPassword;

        /** Only present when success = false */
        private String       errorMessage;
        private List<String> validationErrors;
    }
}
