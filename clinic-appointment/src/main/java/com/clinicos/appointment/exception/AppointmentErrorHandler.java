package com.clinicos.appointment.exception;

import com.clinicos.appointment.controller.AppointmentController;
import com.clinicos.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Doctor checks used to surface as "Internal server error". A wrong or inactive doctor is the
 * user's to fix (400, with the reason); the staff service being unreachable is ours (503).
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AppointmentController.class)
public class AppointmentErrorHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(friendly(ex.getMessage())));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<?>> unavailable(IllegalStateException ex) {
        log.error("Appointment request failed", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Could not check the doctor right now. Please try again in a minute."));
    }

    static String friendly(String message) {
        if (message == null) return "Please check the appointment details.";
        if (message.startsWith("Selected doctor is not active")) return "This doctor is not active. Choose another doctor.";
        if (message.startsWith("Selected staff member is not a doctor")) return "The selected staff member is not a doctor.";
        if (message.startsWith("Doctor not found") || message.startsWith("Doctor does not belong")) return "This doctor was not found in your clinic.";
        return message;
    }
}
