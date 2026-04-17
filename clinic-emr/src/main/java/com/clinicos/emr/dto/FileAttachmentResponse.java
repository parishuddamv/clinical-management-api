package com.clinicos.emr.dto;

import com.clinicos.emr.entity.FileAttachment.FileCategory;
import com.clinicos.emr.entity.FileAttachment.StorageProvider;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileAttachmentResponse {
    private Long id;
    private Long patientId;
    private Long visitId;
    private Long labReportId;
    private FileCategory fileCategory;
    private String fileType;
    private String fileName;
    private String originalFileName;
    private StorageProvider storageProvider;
    private String contentType;
    private Long fileSize;
    private String description;
    private String[] tags;
    private String uploadedBy;
    private String downloadUrl;
    private LocalDateTime createdAt;
}

