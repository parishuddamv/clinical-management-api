package com.clinicos.common.service;

import com.clinicos.common.entity.AuditEvent;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditTrailTest {

    private AuditEvent e(String method, String path, int status) {
        return AuditTrail.describe(method, path, status).orElse(null);
    }

    @Test
    void viewingAPatientIsRecorded() {
        AuditEvent ev = e("GET", "/api/v1/patients/12", 200);
        assertThat(ev.getAction()).isEqualTo("VIEW");
        assertThat(ev.getResourceType()).isEqualTo("patient");
        assertThat(ev.getResourceId()).isEqualTo("12");
    }

    @Test
    void listsAndSearchesAreNotRecorded() {
        assertThat(e("GET", "/api/v1/patients", 200)).isNull();
        assertThat(e("GET", "/api/v1/emr/prescriptions/drugs/search", 200)).isNull();
        assertThat(e("GET", "/api/v1/subscriptions/me", 200)).isNull();
        assertThat(e("GET", "/api/v1/auth/user-status/x", 200)).isNull();
    }

    @Test
    void emrRecordsUseTheirOwnType() {
        AuditEvent ev = e("PUT", "/api/v1/emr/prescriptions/45", 200);
        assertThat(ev.getResourceType()).isEqualTo("prescription");
        assertThat(ev.getAction()).isEqualTo("UPDATE");
        assertThat(e("POST", "/api/v1/emr/prescriptions/45/mark-printed", 200).getAction()).isEqualTo("PRINT");
    }

    @Test
    void patientScopedListsCountAsViewingThatPatient() {
        AuditEvent ev = e("GET", "/api/v1/emr/visits/patient/9", 200);
        assertThat(ev.getResourceType()).isEqualTo("patient");
        assertThat(ev.getResourceId()).isEqualTo("9");
        assertThat(ev.getDetail()).isEqualTo("visits");
    }

    @Test
    void billingAndDenials() {
        assertThat(e("POST", "/api/v1/billing/7/payments", 201).getAction()).isEqualTo("PAYMENT");
        assertThat(e("POST", "/api/v1/billing", 201).getResourceType()).isEqualTo("invoice");
        assertThat(e("GET", "/api/v1/patients/3", 403).getAction()).isEqualTo("DENIED_VIEW");
        assertThat(e("DELETE", "/api/v1/emr/allergies/2", 200).getResourceType()).isEqualTo("allergy");
    }
}
