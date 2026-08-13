package com.clinicos.common.repository;

import com.clinicos.common.entity.ClinicUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

/**
 * Repository for ClinicUser entity
 */
@Repository
public interface ClinicUserRepository extends JpaRepository<ClinicUser, Long> {

    /**
     * Find user by email
     */
    Optional<ClinicUser> findByEmail(String email);

    /**
     * Find user by email (case-insensitive)
     */
    Optional<ClinicUser> findByEmailIgnoreCase(String email);

    /**
     * Find user by email and clinic ID
     */
    Optional<ClinicUser> findByEmailAndClinicId(String email, String clinicId);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find all users by status
     */
    List<ClinicUser> findByStatus(ClinicUser.UserStatus status);

    /**
     * Find all pending users (awaiting approval)
     */
    @Query("SELECT u FROM ClinicUser u WHERE u.status = 'PENDING' ORDER BY u.createdAt ASC")
    List<ClinicUser> findPendingUsers();

    /**
     * Find new users (just registered)
     */
    @Query("SELECT u FROM ClinicUser u WHERE u.status = 'NEW' ORDER BY u.createdAt DESC")
    List<ClinicUser> findNewUsers();

    /**
     * Find users by clinic
     */
    List<ClinicUser> findByClinicId(String clinicId);

    /**
     * Find approved users by clinic
     */
    @Query("SELECT u FROM ClinicUser u WHERE u.clinicId = :clinicId AND u.status = 'APPROVED'")
    List<ClinicUser> findApprovedUsersByClinic(@Param("clinicId") String clinicId);

    /**
     * Count new registrations
     */
    @Query("SELECT COUNT(u) FROM ClinicUser u WHERE u.status = 'NEW'")
    long countNewRegistrations();

    /**
     * Find users registered in last N days
     */
    @Query(value = "SELECT * FROM clinic_users WHERE created_at >= NOW() - CAST(:days || ' days' AS INTERVAL)", nativeQuery = true)
    List<ClinicUser> findRecentRegistrations(@Param("days") int days);
}

