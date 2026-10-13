package com.clinicos.appointment.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class StaffClient {

    private final RestClient.Builder restClientBuilder;

    @Value("${app.services.staff-url:http://localhost:8087}")
    private String staffServiceUrl;

    public StaffClient(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    public StaffMemberInfo getActiveDoctor(
            Long staffId,
            String authorization,
            String clinicId) {

        if (staffId == null) {
            throw new IllegalArgumentException("Doctor ID is required");
        }

        if (authorization == null || authorization.isBlank()) {
            throw new IllegalArgumentException(
                    "Authorization token is required to validate doctor");
        }

        try {
            StaffResponse response = restClientBuilder
                    .baseUrl(staffServiceUrl)
                    .build()
                    .get()
                    .uri("/api/v1/staff/{id}", staffId)
                    .header(HttpHeaders.AUTHORIZATION, authorization)
                    .header("X-Clinic-ID", clinicId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(StaffResponse.class);

            if (response == null || response.data() == null) {
                throw new IllegalArgumentException(
                        "Doctor not found: " + staffId);
            }

            StaffMemberInfo doctor = response.data();

            if (!"DOCTOR".equalsIgnoreCase(doctor.role())) {
                throw new IllegalArgumentException(
                        "Selected staff member is not a doctor: " + staffId);
            }

            if (!"ACTIVE".equalsIgnoreCase(doctor.status())) {
                throw new IllegalArgumentException(
                        "Selected doctor is not active: " + staffId);
            }

            if (doctor.clinicId() != null
                    && !clinicId.equals(doctor.clinicId())) {
                throw new IllegalArgumentException(
                        "Doctor does not belong to the current clinic: " + staffId);
            }

            return doctor;

        } catch (RestClientException ex) {
            throw new IllegalStateException(
                    "Unable to validate doctor with Staff service: " + staffId,
                    ex);
        }
    }

    public record StaffResponse(
            boolean success,
            StaffMemberInfo data
    ) {}

    public record StaffMemberInfo(
            Long id,
            String firstName,
            String lastName,
            String email,
            String phone,
            String profileImageUrl,
            String role,
            String status,
            String specialization,
            String qualification,
            String licenseNumber,
            Integer experienceYears,
            String fullName,
            String clinicId
    ) {}
}
