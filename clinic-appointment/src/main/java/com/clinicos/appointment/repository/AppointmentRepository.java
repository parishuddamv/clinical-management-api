package com.clinicos.appointment.repository;

import com.clinicos.appointment.entity.Appointment;
import com.clinicos.appointment.entity.Appointment.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Page<Appointment> findByClinicId(String clinicId, Pageable pageable);

    Page<Appointment> findByClinicIdAndStatus(String clinicId, AppointmentStatus status, Pageable pageable);

    /**
     * Find appointments for a specific date range (for today's appointments)
     */
    @Query("SELECT a FROM Appointment a WHERE a.clinicId = :clinicId " +
           "AND a.appointmentDateTime >= :startOfDay AND a.appointmentDateTime < :endOfDay " +
           "ORDER BY a.appointmentDateTime ASC")
    Page<Appointment> findByClinicIdAndDateRange(
            @Param("clinicId") String clinicId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            Pageable pageable);
}
