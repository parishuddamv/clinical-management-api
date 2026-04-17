package com.clinicos.notification.dto;

import com.clinicos.notification.entity.Notification.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendNotificationRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Notification type is required")
    private NotificationType type;

    @NotBlank(message = "Recipient is required")
    private String recipient;

    @NotBlank(message = "Message is required")
    private String message;
}
