package com.clinicos.emr.entity;

import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * File Attachment entity for storing X-rays, scans, and documents.
 */
@Entity
@Table(name = "file_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAttachment extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    private PatientVisit visit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_report_id")
    private LabReport labReport;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_category", nullable = false, length = 50)
    private FileCategory fileCategory;

    @Column(name = "file_type", nullable = false, length = 50)
    private String fileType;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 30)
    @Builder.Default
    private StorageProvider storageProvider = StorageProvider.LOCAL;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "checksum", length = 64)
    private String checksum;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "tags", columnDefinition = "TEXT[]")
    private String[] tags;

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    public enum FileCategory {
        XRAY,
        CT_SCAN,
        MRI,
        ULTRASOUND,
        LAB_REPORT,
        ECG,
        PRESCRIPTION,
        CONSENT_FORM,
        INSURANCE_DOCUMENT,
        REFERRAL_LETTER,
        DISCHARGE_SUMMARY,
        OTHER
    }

    public enum StorageProvider {
        LOCAL,
        GCS,
        S3,
        AZURE_BLOB
    }
}

