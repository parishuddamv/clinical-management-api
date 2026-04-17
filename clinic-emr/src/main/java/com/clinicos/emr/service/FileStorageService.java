package com.clinicos.emr.service;

import com.clinicos.emr.dto.*;
import com.clinicos.emr.entity.FileAttachment;
import com.clinicos.emr.entity.FileAttachment.FileCategory;
import com.clinicos.emr.entity.FileAttachment.StorageProvider;
import com.clinicos.emr.repository.FileAttachmentRepository;
import com.clinicos.emr.repository.LabReportRepository;
import com.clinicos.emr.repository.PatientVisitRepository;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final FileAttachmentRepository fileRepository;
    private final PatientVisitRepository visitRepository;
    private final LabReportRepository labReportRepository;

    @Value("${app.file-storage.local.base-path:./uploads}")
    private String uploadBasePath;

    @Transactional
    public FileAttachmentResponse uploadFile(String clinicId, Long patientId, MultipartFile file,
                                              FileCategory category, Long visitId, Long labReportId,
                                              String description, String uploadedBy) {
        try {
            log.info("Uploading file for patient: {} in clinic: {}, category: {}", patientId, clinicId, category);

            // Validate file
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Cannot upload empty file");
            }

            // Create directory structure
            Path uploadDir = Paths.get(uploadBasePath, clinicId, String.valueOf(patientId), category.name().toLowerCase());
            Files.createDirectories(uploadDir);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename);
            String uniqueFilename = UUID.randomUUID().toString() + "." + extension;
            Path targetPath = uploadDir.resolve(uniqueFilename);

            // Copy file
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Calculate checksum
            String checksum = calculateChecksum(file.getBytes());

            // Create entity
            FileAttachment attachment = FileAttachment.builder()
                    .patientId(patientId)
                    .fileCategory(category)
                    .fileType(extension != null ? extension.toUpperCase() : "UNKNOWN")
                    .fileName(uniqueFilename)
                    .originalFileName(originalFilename)
                    .filePath(targetPath.toString())
                    .storageProvider(StorageProvider.LOCAL)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .checksum(checksum)
                    .description(description)
                    .uploadedBy(uploadedBy)
                    .isDeleted(false)
                    .build();

            attachment.setClinicId(clinicId);

            // Link to visit if provided
            if (visitId != null) {
                visitRepository.findByIdAndClinicId(visitId, clinicId)
                        .ifPresent(attachment::setVisit);
            }

            // Link to lab report if provided
            if (labReportId != null) {
                labReportRepository.findByIdAndClinicId(labReportId, clinicId)
                        .ifPresent(attachment::setLabReport);
            }

            attachment = fileRepository.save(attachment);
            log.info("Uploaded file: {} with ID: {}", originalFilename, attachment.getId());

            return mapToResponse(attachment);
        } catch (IOException e) {
            log.error("Error uploading file", e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    @Transactional(readOnly = true)
    public FileAttachmentResponse getFileInfo(String clinicId, Long fileId) {
        FileAttachment attachment = fileRepository.findByIdAndClinicIdAndIsDeletedFalse(fileId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
        return mapToResponse(attachment);
    }

    public Resource downloadFile(String clinicId, Long fileId) {
        FileAttachment attachment = fileRepository.findByIdAndClinicIdAndIsDeletedFalse(fileId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

        try {
            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not readable: " + fileId);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error reading file", e);
        }
    }

    @Transactional
    public void deleteFile(String clinicId, Long fileId) {
        int updated = fileRepository.softDelete(fileId, clinicId);
        if (updated == 0) {
            throw new ResourceNotFoundException("File not found: " + fileId);
        }
        log.info("Soft deleted file: {}", fileId);
    }

    @Transactional(readOnly = true)
    public Page<FileAttachmentResponse> getPatientFiles(String clinicId, Long patientId, Pageable pageable) {
        return fileRepository.findByClinicIdAndPatientIdAndIsDeletedFalseOrderByCreatedAtDesc(clinicId, patientId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<FileAttachmentResponse> getFilesByCategory(String clinicId, FileCategory category, Pageable pageable) {
        return fileRepository.findByClinicIdAndFileCategoryAndIsDeletedFalseOrderByCreatedAtDesc(clinicId, category, pageable)
                .map(this::mapToResponse);
    }

    private String calculateChecksum(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return null;
        }
    }

    private FileAttachmentResponse mapToResponse(FileAttachment f) {
        return FileAttachmentResponse.builder()
                .id(f.getId())
                .patientId(f.getPatientId())
                .visitId(f.getVisit() != null ? f.getVisit().getId() : null)
                .labReportId(f.getLabReport() != null ? f.getLabReport().getId() : null)
                .fileCategory(f.getFileCategory())
                .fileType(f.getFileType())
                .fileName(f.getFileName())
                .originalFileName(f.getOriginalFileName())
                .storageProvider(f.getStorageProvider())
                .contentType(f.getContentType())
                .fileSize(f.getFileSize())
                .description(f.getDescription())
                .tags(f.getTags())
                .uploadedBy(f.getUploadedBy())
                .downloadUrl("/api/v1/emr/files/" + f.getId() + "/download")
                .createdAt(f.getCreatedAt())
                .build();
    }
}

