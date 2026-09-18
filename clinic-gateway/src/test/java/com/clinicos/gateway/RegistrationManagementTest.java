package com.clinicos.gateway;

import com.clinicos.common.dto.RegistrationRequest;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.entity.ClinicUser.UserStatus;
import com.clinicos.common.repository.ClinicUserRepository;
import com.clinicos.common.repository.RegistrationHistoryRepository;
import com.clinicos.common.security.JwtTokenProvider;
import com.clinicos.common.security.RegistrationAccess;
import com.clinicos.common.service.GoogleAuthService;
import com.clinicos.common.service.UserRegistrationService;
import com.clinicos.gateway.config.AuthRouterConfig;
import com.clinicos.gateway.config.GatewaySecurityConfig;
import com.clinicos.gateway.handler.AuthHandler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Real HTTP routing + security + registration service + JPA transactions against isolated H2. */
@SpringBootTest(classes = RegistrationManagementTest.Config.class, properties = {
        "spring.main.web-application-type=reactive", "spring.profiles.active=test",
        "spring.datasource.url=jdbc:h2:mem:registration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false", "spring.cloud.gateway.enabled=false",
        "jwt.secret=registration-test-key-only-not-for-production-abcdefghijklmnopqrstuvwxyz-1234567890",
        "logging.file.name=target/registration-test.log"
})
@AutoConfigureWebTestClient
class RegistrationManagementTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = "org.springframework.cloud.gateway.config.GatewayRedisAutoConfiguration")
    @EntityScan(basePackageClasses = ClinicUser.class)
    @EnableJpaRepositories(basePackageClasses = ClinicUserRepository.class)
    @Import({AuthRouterConfig.class, GatewaySecurityConfig.class, AuthHandler.class,
            UserRegistrationService.class, RegistrationAccess.class, JwtTokenProvider.class})
    static class Config { }

    @Autowired WebTestClient http;
    @Autowired ClinicUserRepository users;
    @org.springframework.boot.test.mock.mockito.SpyBean RegistrationHistoryRepository history;
    @Autowired JwtTokenProvider tokens;
    @Autowired UserRegistrationService registrations;
    @Autowired RegistrationAccess registrationAccess;
    @MockBean GoogleAuthService google;
    private String superToken;

    @BeforeEach
    void setup() {
        history.deleteAll();
        users.deleteAll();
        seed("root@example.com", "ADMIN", UserStatus.APPROVED, true);
        seed("target@example.com", "DOCTOR", UserStatus.NEW, false);
        superToken = bearer("root@example.com");
    }

    private ClinicUser seed(String email, String role, UserStatus status, boolean superAdmin) {
        return users.saveAndFlush(ClinicUser.builder().email(email).fullName("Test Person")
                .phone("9876543210").clinicName("Test Clinic").clinicId("CLINIC_A")
                .role(role).status(status).isActive(status == UserStatus.APPROVED).isSuperAdmin(superAdmin).build());
    }

    private String bearer(String email) { return "Bearer " + tokens.generateToken(email, "CLINIC_A"); }
    private ClinicUser target() { return users.findByEmail("target@example.com").orElseThrow(); }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "DOCTOR", "RECEPTIONIST", "NURSE", "PHARMACIST", "LAB_TECHNICIAN", "ACCOUNTANT", "MANAGER", "BILLING", "SUPER_ADMIN"})
    void allNormalRolesCannotManageRegistrations(String role) {
        seed("caller@example.com", role, UserStatus.APPROVED, false);
        assertManagementForbidden(bearer("caller@example.com"));
        assertThat(target().getStatus()).isEqualTo(UserStatus.NEW);
        assertThat(history.count()).isZero();
    }

    private void assertManagementForbidden(String authorization) {
        for (String action : List.of("approve", "reject", "suspend", "reactivate", "review")) {
            var request = http.put().uri("/api/v1/auth/admin/" + action + "/target@example.com?clinicId=B&approvedBy=root@example.com&rejectionReason=test");
            if (authorization != null) request.header("Authorization", authorization);
            request.exchange().expectStatus().isForbidden().expectBody().jsonPath("$.success").isEqualTo(false);
        }
        for (String path : List.of("/api/v1/auth/admin/registrations", "/api/v1/auth/admin/registrations/target@example.com/history")) {
            var request = http.get().uri(path);
            if (authorization != null) request.header("Authorization", authorization);
            request.exchange().expectStatus().isForbidden();
        }
    }

    @Test void anonymousAndInvalidTokensAreForbidden() {
        assertManagementForbidden(null);
        assertManagementForbidden("Bearer invalid");
    }

    @Test void inactiveOrSuspendedSuperAdminIsForbidden() {
        ClinicUser root = users.findByEmail("root@example.com").orElseThrow();
        root.setStatus(UserStatus.SUSPENDED);
        users.saveAndFlush(root);
        assertManagementForbidden(superToken);
        root.setStatus(UserStatus.APPROVED);
        root.setIsActive(false);
        users.saveAndFlush(root);
        assertManagementForbidden(superToken);
    }

    @Test void serverSideSuperAdminPrivilegeIsSupported() {
        seed("platform@example.com", "SUPER_ADMIN", UserStatus.APPROVED, true);
        http.get().uri("/api/v1/auth/admin/registrations").header("Authorization", bearer("platform@example.com"))
                .exchange().expectStatus().isOk();
    }

    @Test void approvePreservesRoleAndUsesVerifiedActorThenSuspendsAndReactivates() {
        http.put().uri("/api/v1/auth/admin/approve/target@example.com?approvedBy=spoof@example.com")
                .header("Authorization", superToken).exchange().expectStatus().isOk()
                .expectBody().jsonPath("$.data.status").isEqualTo("APPROVED")
                .jsonPath("$.data.isActive").isEqualTo(true).jsonPath("$.data.role").isEqualTo("DOCTOR")
                .jsonPath("$.data.approvedBy").isEqualTo("root@example.com");
        assertThat(target().getApprovedAt()).isNotNull();
        assertThat(target().getClinicId()).isEqualTo("CLINIC_A");
        http.put().uri("/api/v1/auth/admin/approve/target@example.com").header("Authorization", superToken)
                .exchange().expectStatus().isEqualTo(409);
        http.put().uri("/api/v1/auth/admin/suspend/target@example.com").header("Authorization", superToken)
                .exchange().expectStatus().isOk();
        assertThat(target().getIsActive()).isFalse();
        http.put().uri("/api/v1/auth/admin/reactivate/target@example.com").header("Authorization", superToken)
                .exchange().expectStatus().isOk();
        assertThat(target().getIsActive()).isTrue();
        http.get().uri("/api/v1/auth/admin/registrations/target@example.com/history").header("Authorization", superToken)
                .exchange().expectStatus().isOk().expectBody()
                .jsonPath("$.data.totalElements").isEqualTo(3)
                .jsonPath("$.data.content[0].action").isEqualTo("REACTIVATED")
                .jsonPath("$.data.content[0].previousStatus").isEqualTo("SUSPENDED");
    }

    @Test void rejectionRequiresReasonPreservesRecordAndCanBeReopened() {
        Long id = target().getId();
        http.put().uri("/api/v1/auth/admin/reject/target@example.com").header("Authorization", superToken)
                .exchange().expectStatus().isBadRequest();
        http.put().uri(b -> b.path("/api/v1/auth/admin/reject/target@example.com")
                        .queryParam("rejectionReason", "Incomplete documents").build())
                .header("Authorization", superToken).exchange().expectStatus().isOk();
        assertThat(target().getId()).isEqualTo(id);
        assertThat(target().getStatus()).isEqualTo(UserStatus.REJECTED);
        assertThat(target().getRejectionReason()).isEqualTo("Incomplete documents");
        assertThat(target().getIsActive()).isFalse();
        http.put().uri("/api/v1/auth/admin/review/target@example.com").header("Authorization", superToken)
                .exchange().expectStatus().isOk();
        assertThat(target().getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(history.findAll()).anySatisfy(h -> {
            assertThat(h.getAction()).isEqualTo("REJECTED");
            assertThat(h.getRejectionReason()).isEqualTo("Incomplete documents");
        });
    }

    @Test void listingFiltersAndSearchesByNameEmailAndClinic() {
        for (String query : List.of("Test Person", "target@", "Test Clinic", "CLINIC_A")) {
            http.get().uri(b -> b.path("/api/v1/auth/admin/registrations").queryParam("status", "NEW")
                            .queryParam("search", query).build()).header("Authorization", superToken)
                    .exchange().expectStatus().isOk().expectBody().jsonPath("$.data.totalElements").isEqualTo(1);
        }
        http.get().uri("/api/v1/auth/admin/registrations?status=INVALID").header("Authorization", superToken)
                .exchange().expectStatus().isBadRequest();
        http.get().uri("/api/v1/auth/admin/registrations?size=101").header("Authorization", superToken)
                .exchange().expectStatus().isBadRequest();
        http.get().uri(b -> b.path("/api/v1/auth/admin/registrations").queryParam("search", "%").build()).header("Authorization", superToken)
                .exchange().expectStatus().isOk().expectBody().jsonPath("$.data.totalElements").isEqualTo(0);
    }

    @Test void existingDetailsAndStatusRequireSelfOrSuperAdmin() {
        seed("other@example.com", "ADMIN", UserStatus.APPROVED, false);
        for (String resource : List.of("user", "user-status", "check-approval")) {
            String path = "/api/v1/auth/" + resource + "/target@example.com";
            http.get().uri(path).exchange().expectStatus().isForbidden();
            http.get().uri(path).header("Authorization", bearer("other@example.com")).exchange().expectStatus().isForbidden();
            http.get().uri(path).header("Authorization", bearer("target@example.com")).exchange().expectStatus().isOk();
            http.get().uri(path).header("Authorization", superToken).exchange().expectStatus().isOk();
        }
    }

    private RegistrationRequest validRequest() {
        return RegistrationRequest.builder().email("new@example.com").fullName("New Doctor").phone("+91 9876543210")
                .clinicName("New Clinic").role("DOCTOR").additionalNotes("Preserve this information").build();
    }

    @Test void registersValidatesAndPreservesCompleteDetails() {
        http.post().uri("/api/v1/auth/register").bodyValue(validRequest()).exchange().expectStatus().isCreated()
                .expectBody().jsonPath("$.data.isActive").isEqualTo(false);
        ClinicUser user = users.findByEmail("new@example.com").orElseThrow();
        assertThat(user.getRegistrationDetails()).contains("Preserve this information");
        assertThat(user.getIsSuperAdmin()).isFalse();
        assertThat(history.findAll()).singleElement().satisfies(h -> assertThat(h.getAction()).isEqualTo("REGISTERED"));
        http.post().uri("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).bodyValue("{")
                .exchange().expectStatus().isBadRequest();
        http.post().uri("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .exchange().expectStatus().isBadRequest();
    }

    @ParameterizedTest
    @ValueSource(strings = {"fullName", "email", "phone", "clinicName", "role"})
    void invalidFieldsReturn400(String field) {
        var request = validRequest();
        switch (field) {
            case "fullName" -> request.setFullName(" ");
            case "email" -> request.setEmail("not-email");
            case "phone" -> request.setPhone("+++++++");
            case "clinicName" -> request.setClinicName(" ");
            case "role" -> request.setRole("SUPER_ADMIN");
        }
        http.post().uri("/api/v1/auth/register").bodyValue(request).exchange().expectStatus().isBadRequest();
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEW", "PENDING", "APPROVED", "REJECTED", "SUSPENDED"})
    void duplicateEmailIsCaseInsensitiveAndNeverOverwrites(String status) {
        ClinicUser user = target();
        user.setStatus(UserStatus.valueOf(status));
        users.saveAndFlush(user);
        var request = validRequest();
        request.setEmail(" TARGET@EXAMPLE.COM ");
        http.post().uri("/api/v1/auth/register").bodyValue(request).exchange().expectStatus().isEqualTo(409);
        assertThat(target().getStatus()).isEqualTo(UserStatus.valueOf(status));
        assertThat(target().getFullName()).isEqualTo("Test Person");
        assertThat(users.count()).isEqualTo(2);
    }

    @Test void newClinicAssociationIsGeneratedAndInvalidTransitionsConflict() {
        ClinicUser user = target();
        user.setClinicId(null);
        users.saveAndFlush(user);
        http.put().uri("/api/v1/auth/admin/suspend/target@example.com").header("Authorization", superToken)
                .exchange().expectStatus().isEqualTo(409);
        http.put().uri("/api/v1/auth/admin/approve/target@example.com").header("Authorization", superToken)
                .exchange().expectStatus().isOk();
        assertThat(target().getClinicId()).startsWith("CLINIC_");
        http.put().uri("/api/v1/auth/admin/reject/target@example.com?rejectionReason=late")
                .header("Authorization", superToken).exchange().expectStatus().isEqualTo(409);
    }

    @Test void auditFailureRollsBackDecisionAndDoesNotLeakInternals() {
        org.mockito.Mockito.doThrow(new IllegalStateException("database-password-must-not-leak"))
                .when(history).save(org.mockito.ArgumentMatchers.any());
        http.put().uri("/api/v1/auth/admin/approve/target@example.com").header("Authorization", superToken)
                .exchange().expectStatus().is5xxServerError().expectBody()
                .jsonPath("$.message").isEqualTo("Unable to approve registration.");
        assertThat(target().getStatus()).isEqualTo(UserStatus.NEW);
        assertThat(history.count()).isZero();
    }

    @Test void competingDecisionsProduceOnlyOneTransition() throws Exception {
        var start = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Boolean> approve = () -> {
                start.await();
                try { registrations.approveUser("target@example.com", null, superToken); return true; }
                catch (com.clinicos.common.exception.RegistrationException e) { return false; }
            };
            java.util.concurrent.Callable<Boolean> reject = () -> {
                start.await();
                try { registrations.rejectUser("target@example.com", "Review failed", superToken); return true; }
                catch (com.clinicos.common.exception.RegistrationException e) { return false; }
            };
            var first = pool.submit(approve);
            var second = pool.submit(reject);
            start.countDown();
            assertThat(List.of(first.get(15, java.util.concurrent.TimeUnit.SECONDS), second.get(15, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(history.count()).isEqualTo(1);
    }

    @Test void serviceMethodsCannotBypassHttpAuthorization() {
        seed("caller@example.com", "ADMIN", UserStatus.APPROVED, false);
        String token = bearer("caller@example.com");
        List<Runnable> operations = List.of(
                () -> registrations.approveUser("target@example.com", "B", token),
                () -> registrations.rejectUser("target@example.com", "reason", token),
                () -> registrations.suspendUser("target@example.com", token),
                () -> registrations.reactivateUser("target@example.com", token),
                () -> registrations.submitForReview("target@example.com", token),
                () -> registrations.registrations(token, null, null, 0, 20),
                () -> registrations.history("target@example.com", token, 0, 20));
        for (Runnable operation : operations) {
            org.assertj.core.api.Assertions.assertThatThrownBy(operation::run)
                    .isInstanceOf(com.clinicos.common.exception.RegistrationException.class);
        }
        assertThat(history.count()).isZero();
    }

    @Test void businessAccessRequiresCurrentApprovedMembershipEvenWithExistingToken() {
        String token = bearer("target@example.com");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registrationAccess.requireClinicAccess(token, "CLINIC_A"))
                .isInstanceOf(com.clinicos.common.exception.RegistrationException.class);
        registrations.approveUser("target@example.com", "CLINIC_A", superToken);
        assertThat(registrationAccess.requireClinicAccess(token, "CLINIC_A").getEmail()).isEqualTo("target@example.com");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registrationAccess.requireClinicAccess(token, "CLINIC_B"))
                .isInstanceOf(com.clinicos.common.exception.RegistrationException.class);
        registrations.suspendUser("target@example.com", superToken);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> registrationAccess.requireClinicAccess(token, "CLINIC_A"))
                .isInstanceOf(com.clinicos.common.exception.RegistrationException.class);
    }
}
