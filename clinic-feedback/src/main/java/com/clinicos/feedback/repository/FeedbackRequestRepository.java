package com.clinicos.feedback.repository;

import com.clinicos.feedback.entity.FeedbackRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRequestRepository extends JpaRepository<FeedbackRequest, Long> {

    Optional<FeedbackRequest> findByFeedbackToken(String token);

    Optional<FeedbackRequest> findByIdAndClinicId(Long id, String clinicId);

    Page<FeedbackRequest> findByClinicIdAndIsCompletedFalseAndExpiresAtAfterOrderBySentAtDesc(
            String clinicId, LocalDateTime now, Pageable pageable);

    @Query("SELECT fr FROM FeedbackRequest fr WHERE fr.clinicId = :clinicId AND fr.isCompleted = false " +
           "AND fr.expiresAt > :now AND fr.reminderCount < :maxReminders " +
           "AND (fr.lastReminderAt IS NULL OR fr.lastReminderAt < :reminderCutoff)")
    List<FeedbackRequest> findPendingForReminder(@Param("clinicId") String clinicId,
                                                  @Param("now") LocalDateTime now,
                                                  @Param("maxReminders") int maxReminders,
                                                  @Param("reminderCutoff") LocalDateTime reminderCutoff);

    boolean existsByAppointmentIdAndClinicId(Long appointmentId, String clinicId);

    boolean existsByVisitIdAndClinicId(Long visitId, String clinicId);
}

