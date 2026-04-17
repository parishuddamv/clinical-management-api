package com.clinicos.notification.controller;

import com.clinicos.notification.dto.NotificationResponse;
import com.clinicos.notification.dto.SendNotificationRequest;
import com.clinicos.notification.entity.Notification.NotificationStatus;
import com.clinicos.notification.entity.Notification.NotificationType;
import com.clinicos.notification.service.NotificationService;
import com.clinicos.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * POST /api/v1/notifications/send
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<NotificationResponse>> send(
            @Valid @RequestBody SendNotificationRequest request,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        NotificationResponse response = notificationService.send(clinicId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification sent", response));
    }

    /**
     * GET /api/v1/notifications?type=&status=&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getAll(
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        String clinicId = getClinicId(authentication);
        Page<NotificationResponse> results = notificationService.getAll(
                clinicId, type, status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    private String getClinicId(Authentication authentication) {
        return (String) authentication.getDetails();
    }
}
