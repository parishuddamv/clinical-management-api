package com.clinicos.staff.repository;

import com.clinicos.staff.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByClinicIdAndRole(String clinicId, String role);

    @Query("SELECT rp.permission FROM RolePermission rp WHERE " +
           "(rp.clinicId = :clinicId OR rp.clinicId = 'DEFAULT') AND rp.role = :role")
    Set<String> findPermissionsByRole(@Param("clinicId") String clinicId, @Param("role") String role);

    void deleteByClinicIdAndRole(String clinicId, String role);

    boolean existsByClinicIdAndRoleAndPermission(String clinicId, String role, String permission);
}

