package com.clinicos.emr.repository;

import com.clinicos.emr.entity.DrugMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DrugMasterRepository extends JpaRepository<DrugMaster, Long> {

    @Query("SELECT d FROM DrugMaster d WHERE d.isActive = true " +
           "AND (d.clinicId IS NULL OR d.clinicId = :clinicId) " +
           "AND (LOWER(d.brandName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(d.genericName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY d.brandName ASC")
    List<DrugMaster> searchDrugs(@Param("clinicId") String clinicId, @Param("query") String query, Pageable pageable);

    @Query("SELECT d FROM DrugMaster d WHERE d.isActive = true " +
           "AND (d.clinicId IS NULL OR d.clinicId = :clinicId) " +
           "AND d.category = :category ORDER BY d.brandName ASC")
    List<DrugMaster> findByCategory(@Param("clinicId") String clinicId, @Param("category") String category);

    @Query("SELECT DISTINCT d.category FROM DrugMaster d WHERE d.isActive = true " +
           "AND (d.clinicId IS NULL OR d.clinicId = :clinicId) " +
           "AND d.category IS NOT NULL ORDER BY d.category")
    List<String> findAllCategories(@Param("clinicId") String clinicId);

    Page<DrugMaster> findByIsActiveTrueOrderByBrandNameAsc(Pageable pageable);

    @Query("SELECT d FROM DrugMaster d WHERE d.isActive = true " +
           "AND (d.clinicId IS NULL OR d.clinicId = :clinicId) " +
           "ORDER BY d.brandName ASC")
    Page<DrugMaster> findAllActiveDrugs(@Param("clinicId") String clinicId, Pageable pageable);
}

