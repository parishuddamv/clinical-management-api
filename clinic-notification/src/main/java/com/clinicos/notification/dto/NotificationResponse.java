package com.clinicos.notification.dto;

import com.clinicos.notification.entity.Notification;
import com.clinicos.notification.entity.Notification.NotificationStatus;
import com.clinicos.notification.entity.Notification.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private Long id;
    private Long patientId;
    private NotificationType type;
    private String recipient;
    private String message;
    private NotificationStatus status;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .patientId(notification.getPatientId())
                .type(notification.getType())
                .recipient(notification.getRecipient())
                .message(notification.getMessage())
                .status(notification.getStatus())
                .errorMessage(notification.getErrorMessage())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .build();
    }
}
