package com.clinicos.common.security;

import org.junit.jupiter.api.Test;

import static com.clinicos.common.security.RolePolicy.Page.*;
import static org.assertj.core.api.Assertions.assertThat;

class RolePolicyTest {

    private static boolean allowed(String role, String method, String path) {
        return RolePolicy.refusal(role, true, method, path).isEmpty();
    }

    @Test
    void ownersSeeEverythingWhateverTheirProfession() {
        assertThat(RolePolicy.pages("DOCTOR", false)).contains(BILLING, STAFF, SUBSCRIPTION, AUDIT_LOG);
        assertThat(RolePolicy.refusal("DOCTOR", false, "DELETE", "/api/v1/billing/12")).isEmpty();
        assertThat(RolePolicy.pages("ADMIN", true)).containsAll(RolePolicy.pages("MANAGER", true));
    }

    @Test
    void doctorsGetClinicalPagesButNotBillingOrAdmin() {
        assertThat(allowed("DOCTOR", "GET", "/api/v1/patients/9")).isTrue();
        assertThat(allowed("DOCTOR", "POST", "/api/v1/emr/prescriptions")).isTrue();
        assertThat(allowed("DOCTOR", "PUT", "/api/v1/appointments/4")).isTrue();
        assertThat(allowed("DOCTOR", "GET", "/api/v1/staff/doctors")).isTrue();
        assertThat(allowed("DOCTOR", "GET", "/api/v1/billing")).isFalse();
        assertThat(allowed("DOCTOR", "POST", "/api/v1/staff")).isFalse();
        assertThat(allowed("DOCTOR", "PUT", "/api/v1/clinic/profile")).isFalse();
        assertThat(allowed("DOCTOR", "GET", "/api/v1/clinic/audit-log")).isFalse();
        assertThat(allowed("DOCTOR", "GET", "/api/v1/clinic/profile")).isTrue();
        assertThat(RolePolicy.refusal("DOCTOR", true, "GET", "/api/v1/billing")).get().asString()
                .isEqualTo("Your role (Doctor) doesn't include billing. Ask the clinic owner if you need access.");
    }

    @Test
    void nursesViewPrescriptionsButCannotWriteThem() {
        assertThat(allowed("NURSE", "GET", "/api/v1/emr/prescriptions/5")).isTrue();
        assertThat(allowed("NURSE", "POST", "/api/v1/emr/prescriptions")).isFalse();
        assertThat(allowed("NURSE", "POST", "/api/v1/emr/vitals")).isTrue();
        assertThat(allowed("NURSE", "POST", "/api/v1/emr/visits")).isFalse();
    }

    @Test
    void receptionHandlesFrontDeskButNotClinicalRecords() {
        assertThat(allowed("RECEPTIONIST", "POST", "/api/v1/billing/3/payments")).isTrue();
        assertThat(allowed("RECEPTIONIST", "PATCH", "/api/v1/clinic/booking-requests/5")).isTrue();
        assertThat(allowed("RECEPTIONIST", "POST", "/api/v1/patients")).isTrue();
        assertThat(allowed("RECEPTIONIST", "GET", "/api/v1/emr/visits/2")).isFalse();
        assertThat(allowed("RECEPTIONIST", "GET", "/api/v1/emr/prescriptions/2")).isFalse();
    }

    @Test
    void narrowRoles() {
        assertThat(allowed("PHARMACIST", "GET", "/api/v1/emr/prescriptions/2")).isTrue();
        assertThat(allowed("PHARMACIST", "POST", "/api/v1/emr/prescriptions/2/mark-printed")).isTrue();
        assertThat(allowed("PHARMACIST", "PUT", "/api/v1/emr/prescriptions/2")).isFalse();
        assertThat(allowed("PHARMACIST", "POST", "/api/v1/patients")).isFalse();
        assertThat(allowed("LAB_TECHNICIAN", "POST", "/api/v1/emr/lab-reports")).isTrue();
        assertThat(allowed("LAB_TECHNICIAN", "GET", "/api/v1/appointments")).isFalse();
        assertThat(allowed("ACCOUNTANT", "GET", "/api/v1/billing")).isTrue();
        assertThat(allowed("MANAGER", "POST", "/api/v1/subscriptions/upgrade")).isFalse();
        assertThat(allowed("MANAGER", "PUT", "/api/v1/clinic/profile")).isTrue();
        assertThat(allowed("UNKNOWN", "GET", "/api/v1/patients")).isFalse();
    }

    @Test
    void signInPublicAndPreflightAreNotGoverned() {
        assertThat(allowed("PHARMACIST", "GET", "/api/v1/auth/user-status/a@b.c")).isTrue();
        assertThat(allowed("PHARMACIST", "GET", "/api/v1/public/clinics/zest")).isTrue();
        assertThat(allowed("PHARMACIST", "OPTIONS", "/api/v1/billing")).isTrue();
        assertThat(allowed("PHARMACIST", "POST", "/api/v1/clinic/audit-log/events")).isTrue();
        assertThat(allowed("PHARMACIST", "GET", "/api/v1/clinic/me")).isTrue();
    }
}
