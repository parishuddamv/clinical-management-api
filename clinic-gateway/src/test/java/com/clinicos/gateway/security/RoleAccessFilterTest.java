package com.clinicos.gateway.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleAccessFilterTest {

    private static RoleAccessFilter.Member m(String clinic, String role, boolean invited, boolean active) {
        return new RoleAccessFilter.Member(clinic, role, invited, active, 0);
    }

    @Test
    void ownersPassEverywhereAndStaffAreLimitedToTheirRole() {
        assertThat(RoleAccessFilter.decide(m("C1", "DOCTOR", false, true), "C1", "GET", "/api/v1/billing")).isEmpty();
        assertThat(RoleAccessFilter.decide(m("C1", "DOCTOR", true, true), "C1", "GET", "/api/v1/billing")).get().asString().contains("billing");
        assertThat(RoleAccessFilter.decide(m("C1", "DOCTOR", true, true), "C1", "GET", "/api/v1/patients")).isEmpty();
    }

    @Test
    void removedOrMovedAccountsCannotKeepUsingAnOldToken() {
        assertThat(RoleAccessFilter.decide(m("C1", "DOCTOR", true, false), "C1", "GET", "/api/v1/patients")).get().asString().startsWith("Your access");
        assertThat(RoleAccessFilter.decide(m("C2", "DOCTOR", false, true), "C1", "GET", "/api/v1/patients")).get().asString().startsWith("Your access");
        assertThat(RoleAccessFilter.decide(null, "C1", "GET", "/api/v1/patients")).isEmpty();
    }
}
