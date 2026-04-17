package com.clinicos.emr.controller;

import com.clinicos.emr.dto.*;
import com.clinicos.emr.entity.FileAttachment.FileCategory;
import com.clinicos.emr.service.FileStorageService;
import com.clinicos.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/emr/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * Upload a file (X-ray, scan, document)
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileAttachmentResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long patientId,
            @RequestParam FileCategory category,
            @RequestParam(required = false) Long visitId,
            @RequestParam(required = false) Long labReportId,
            @RequestParam(required = false) String description,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        String uploadedBy = authentication.getName();

        FileAttachmentResponse response = fileStorageService.uploadFile(
                clinicId, patientId, file, category, visitId, labReportId, description, uploadedBy);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded successfully", response));
    }

    /**
     * Get file info
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FileAttachmentResponse>> getFileInfo(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        FileAttachmentResponse response = fileStorageService.getFileInfo(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Download file
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        FileAttachmentResponse fileInfo = fileStorageService.getFileInfo(clinicId, id);
        Resource resource = fileStorageService.downloadFile(clinicId, id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileInfo.getContentType() != null ? 
                        fileInfo.getContentType() : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"" + fileInfo.getOriginalFileName() + "\"")
                .body(resource);
    }

    /**
     * Delete file (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable Long id,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        fileStorageService.deleteFile(clinicId, id);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
    }

    /**
     * Get files for a patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<ApiResponse<Page<FileAttachmentResponse>>> getPatientFiles(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<FileAttachmentResponse> results = fileStorageService.getPatientFiles(
                clinicId, patientId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    /**
     * Get files by category (e.g., all X-rays)
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<Page<FileAttachmentResponse>>> getFilesByCategory(
            @PathVariable FileCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<FileAttachmentResponse> results = fileStorageService.getFilesByCategory(
                clinicId, category, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}

