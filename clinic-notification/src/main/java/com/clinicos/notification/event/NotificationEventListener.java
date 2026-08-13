package com.clinicos.notification.event;

import com.clinicos.notification.dto.SendNotificationRequest;
import com.clinicos.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka listener that turns async notification events into persisted +
 * dispatched notifications. Enabled only when {@code app.kafka.enabled=true}
 * so the service keeps working without a broker (falls back to REST intake).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class NotificationEventListener {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${app.kafka.topics.notifications:clinicos.notifications}",
            groupId = "${spring.kafka.consumer.group-id:clinic-notification}",
            concurrency = "${app.kafka.listener-concurrency:3}")
    public void onNotificationEvent(NotificationEvent event) {
        if (event == null || event.getClinicId() == null) {
            log.warn("Skipping malformed notification event: {}", event);
            return;
        }

        SendNotificationRequest request = new SendNotificationRequest();
        request.setPatientId(event.getPatientId());
        request.setType(event.getType());
        request.setRecipient(event.getRecipient());
        request.setMessage(event.getMessage());

        notificationService.send(event.getClinicId(), request);
        log.debug("Processed notification event for clinic={} patient={}",
                event.getClinicId(), event.getPatientId());
    }
}

