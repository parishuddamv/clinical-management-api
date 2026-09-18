package com.clinicos.gateway;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.*;

class RegistrationMigrationTest {
    private Flyway migration(DataSource source, String target) {
        return Flyway.configure().dataSource(source).locations("classpath:db/migration")
                .table("flyway_schema_history_gateway").target(target).load();
    }

    private void seed(JdbcTemplate jdbc, String email) {
        jdbc.update("INSERT INTO clinic_users(email, full_name, role, phone, status) VALUES (?, 'Existing user', 'DOCTOR', '9876543210', 'NEW')", email);
    }

    @Test void migratesExistingUsersWithoutInventingHistoryAndEnforcesNormalizedUniqueness() throws Exception {
        try (var postgres = EmbeddedPostgres.builder().setPort(0).start()) {
            DataSource source = postgres.getPostgresDatabase();
            migration(source, "1").migrate();
            var jdbc = new JdbcTemplate(source);
            seed(jdbc, " Existing@Example.com ");
            Flyway flyway = migration(source, "2");
            flyway.migrate();
            flyway.validate();
            assertThat(jdbc.queryForObject("SELECT email FROM clinic_users", String.class)).isEqualTo("existing@example.com");
            assertThat(jdbc.queryForObject("SELECT action FROM registration_status_history", String.class)).isEqualTo("MIGRATED");
            assertThatThrownBy(() -> seed(jdbc, "EXISTING@example.com"))
                    .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
            assertThat(flyway.migrate().migrationsExecuted).isZero();
        }
    }

    @Test void existingCaseVariantDuplicatesBlockMigrationWithoutDeletingData() throws Exception {
        try (var postgres = EmbeddedPostgres.builder().setPort(0).start()) {
            DataSource source = postgres.getPostgresDatabase();
            migration(source, "1").migrate();
            var jdbc = new JdbcTemplate(source);
            seed(jdbc, "Same@example.com");
            seed(jdbc, "same@example.com");
            assertThatThrownBy(() -> migration(source, "2").migrate())
                    .isInstanceOf(org.flywaydb.core.api.FlywayException.class)
                    .hasMessageContaining("manual reconciliation");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM clinic_users", Integer.class)).isEqualTo(2);
        }
    }
}
