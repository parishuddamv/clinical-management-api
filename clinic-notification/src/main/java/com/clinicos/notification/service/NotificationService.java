package com.clinicos.notification.service;

import com.clinicos.notification.dto.NotificationResponse;
import com.clinicos.notification.dto.SendNotificationRequest;
import com.clinicos.notification.entity.Notification;
import com.clinicos.notification.entity.Notification.NotificationStatus;
import com.clinicos.notification.entity.Notification.NotificationType;
import com.clinicos.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public NotificationResponse send(String clinicId, SendNotificationRequest request) {
        Notification notification = Notification.builder()
                .patientId(request.getPatientId())
                .type(request.getType())
                .recipient(request.getRecipient())
                .message(request.getMessage())
                .status(NotificationStatus.PENDING)
                .build();
        notification.setClinicId(clinicId);

        Notification saved = notificationRepository.save(notification);

        // Dispatch notification (fire-and-forget; update status accordingly)
        try {
            dispatchNotification(saved);
            saved.setStatus(NotificationStatus.SENT);
            notificationRepository.save(saved);
        } catch (Exception ex) {
            log.error("Failed to send notification id={}: {}", saved.getId(), ex.getMessage());
            saved.setStatus(NotificationStatus.FAILED);
            saved.setErrorMessage(ex.getMessage());
            notificationRepository.save(saved);
        }

        return NotificationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAll(
            String clinicId, NotificationType type, NotificationStatus status, Pageable pageable) {
        Page<Notification> page;
        if (type != null && status != null) {
            page = notificationRepository.findByClinicIdAndTypeAndStatus(clinicId, type, status, pageable);
        } else if (type != null) {
            page = notificationRepository.findByClinicIdAndType(clinicId, type, pageable);
        } else if (status != null) {
            page = notificationRepository.findByClinicIdAndStatus(clinicId, status, pageable);
        } else {
            page = notificationRepository.findByClinicId(clinicId, pageable);
        }
        return page.map(NotificationResponse::from);
    }

    /**
     * Actual dispatch logic — extend this for email/SMS/push integrations.
     */
    private void dispatchNotification(Notification notification) {
        log.info("Dispatching {} notification to {} for patient {}",
                notification.getType(), notification.getRecipient(), notification.getPatientId());
        // TODO: Integrate with email (SMTP), SMS (Twilio/SNS), or push notification providers
    }
}
