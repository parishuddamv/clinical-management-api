package com.clinicos.notification.event;

import com.clinicos.notification.entity.Notification.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event payload consumed from the {@code clinicos.notifications} Kafka topic.
 * Producers (appointment, billing, EMR, etc.) publish this JSON structure to
 * request an asynchronous notification dispatch.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private String clinicId;
    private Long patientId;
    private NotificationType type;
    private String recipient;
    private String message;
}

