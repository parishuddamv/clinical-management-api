package com.clinicos.notification.repository;

import com.clinicos.notification.entity.Notification;
import com.clinicos.notification.entity.Notification.NotificationStatus;
import com.clinicos.notification.entity.Notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByClinicId(String clinicId, Pageable pageable);

    Page<Notification> findByClinicIdAndStatus(String clinicId, NotificationStatus status, Pageable pageable);

    Page<Notification> findByClinicIdAndType(String clinicId, NotificationType type, Pageable pageable);

    Page<Notification> findByClinicIdAndTypeAndStatus(String clinicId, NotificationType type, NotificationStatus status, Pageable pageable);
}
