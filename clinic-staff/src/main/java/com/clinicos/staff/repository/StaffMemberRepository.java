package com.clinicos.staff.repository;

import com.clinicos.staff.entity.StaffMember;
import com.clinicos.staff.entity.StaffMember.StaffRole;
import com.clinicos.staff.entity.StaffMember.StaffStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {

    Optional<StaffMember> findByIdAndClinicId(Long id, String clinicId);

    Optional<StaffMember> findByEmailAndClinicId(String email, String clinicId);

    Optional<StaffMember> findByUserIdAndClinicId(String userId, String clinicId);

    Page<StaffMember> findByClinicIdOrderByFirstNameAsc(String clinicId, Pageable pageable);

    Page<StaffMember> findByClinicIdAndRoleOrderByFirstNameAsc(String clinicId, StaffRole role, Pageable pageable);

    Page<StaffMember> findByClinicIdAndStatusOrderByFirstNameAsc(String clinicId, StaffStatus status, Pageable pageable);

    List<StaffMember> findByClinicIdAndRoleAndStatus(String clinicId, StaffRole role, StaffStatus status);

    @Query("SELECT s FROM StaffMember s WHERE s.clinicId = :clinicId AND s.role = 'DOCTOR' AND s.status = 'ACTIVE' ORDER BY s.firstName")
    List<StaffMember> findActiveDoctors(@Param("clinicId") String clinicId);

    @Query("SELECT s FROM StaffMember s WHERE s.clinicId = :clinicId AND " +
           "(LOWER(s.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.email) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<StaffMember> searchStaff(@Param("clinicId") String clinicId, @Param("query") String query, Pageable pageable);

    long countByClinicIdAndRole(String clinicId, StaffRole role);

    long countByClinicIdAndStatus(String clinicId, StaffStatus status);

    boolean existsByEmailAndClinicId(String email, String clinicId);
}

