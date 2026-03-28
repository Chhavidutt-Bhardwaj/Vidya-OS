package com.ai.vidya.modules.school.entity;

import com.ai.vidya.common.entity.BaseEntity;
import com.ai.vidya.common.enums.DocumentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * A document uploaded by / for the school during onboarding or later.
 * Examples: affiliation certificate, UDISE letter, logo, government NOC.
 *
 * Many-to-one with School.
 */
@Entity
@Table(
    name = "school_documents",
    indexes = {
        @Index(name = "idx_doc_school_id",   columnList = "school_id"),
        @Index(name = "idx_doc_type",        columnList = "document_type"),
        @Index(name = "idx_doc_verified",    columnList = "verified")
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    /** Human-readable label for the document */
    @Column(name = "document_name", nullable = false, length = 255)
    private String documentName;

    /** S3 / CDN storage key or URL */
    @Column(name = "file_url", nullable = false, length = 512)
    private String fileUrl;

    /** MIME type e.g. "application/pdf", "image/jpeg" */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /** File size in bytes */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /** Original file name uploaded by the user */
    @Column(name = "original_file_name", length = 255)
    private String originalFileName;

    /** Whether the document has been verified by an admin */
    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    /** When the document expires (if applicable) */
    @Column(name = "expires_on")
    private LocalDate expiresOn;

    /** Admin notes during verification */
    @Column(name = "admin_remarks", columnDefinition = "TEXT")
    private String adminRemarks;
}