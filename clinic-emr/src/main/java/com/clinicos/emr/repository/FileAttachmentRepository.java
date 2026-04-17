package com.clinicos.emr.repository;

import com.clinicos.emr.entity.FileAttachment;
import com.clinicos.emr.entity.FileAttachment.FileCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileAttachmentRepository extends JpaRepository<FileAttachment, Long> {

    Optional<FileAttachment> findByIdAndClinicIdAndIsDeletedFalse(Long id, String clinicId);

    Page<FileAttachment> findByClinicIdAndIsDeletedFalseOrderByCreatedAtDesc(String clinicId, Pageable pageable);

    Page<FileAttachment> findByClinicIdAndPatientIdAndIsDeletedFalseOrderByCreatedAtDesc(
            String clinicId, Long patientId, Pageable pageable);

    List<FileAttachment> findByVisitIdAndIsDeletedFalseOrderByCreatedAtDesc(Long visitId);

    List<FileAttachment> findByLabReportIdAndIsDeletedFalseOrderByCreatedAtDesc(Long labReportId);

    Page<FileAttachment> findByClinicIdAndFileCategoryAndIsDeletedFalseOrderByCreatedAtDesc(
            String clinicId, FileCategory fileCategory, Pageable pageable);

    @Query("SELECT f FROM FileAttachment f WHERE f.clinicId = :clinicId " +
           "AND f.patientId = :patientId AND f.fileCategory IN :categories " +
           "AND f.isDeleted = false ORDER BY f.createdAt DESC")
    List<FileAttachment> findByPatientAndCategories(
            @Param("clinicId") String clinicId,
            @Param("patientId") Long patientId,
            @Param("categories") List<FileCategory> categories);

    @Modifying
    @Query("UPDATE FileAttachment f SET f.isDeleted = true WHERE f.id = :id AND f.clinicId = :clinicId")
    int softDelete(@Param("id") Long id, @Param("clinicId") String clinicId);

    @Query("SELECT SUM(f.fileSize) FROM FileAttachment f WHERE f.clinicId = :clinicId AND f.isDeleted = false")
    Long getTotalStorageUsed(@Param("clinicId") String clinicId);
}

