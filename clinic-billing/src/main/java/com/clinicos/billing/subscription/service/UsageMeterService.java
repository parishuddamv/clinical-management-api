package com.clinicos.billing.subscription.service;

import com.clinicos.billing.subscription.model.LimitKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageMeterService {

    private final JdbcTemplate jdbcTemplate;

    public long getCurrentUsage(String clinicId, LimitKey limitKey) {
        return switch (limitKey) {
            case DOCTORS -> queryCount(
                    "SELECT COUNT(*) FROM staff_members WHERE clinic_id = ? AND role = 'DOCTOR' AND status = 'ACTIVE'",
                    clinicId
            );
            case STAFF -> queryCount(
                    "SELECT COUNT(*) FROM staff_members WHERE clinic_id = ? AND status = 'ACTIVE'",
                    clinicId
            );
            case LOCATIONS -> countLocations(clinicId);
        };
    }

    private long countLocations(String clinicId) {
        if (tableExists("clinic_locations")) {
            return queryCount("SELECT COUNT(*) FROM clinic_locations WHERE clinic_id = ?", clinicId);
        }
        // Current architecture uses one clinicId per tenant unless clinic_locations is introduced.
        return 1L;
    }

    private boolean tableExists(String tableName) {
        try {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                    Integer.class,
                    tableName
            );
            return exists != null && exists > 0;
        } catch (DataAccessException ex) {
            log.warn("Unable to verify table existence for {}: {}", tableName, ex.getMessage());
            return false;
        }
    }

    private long queryCount(String sql, String clinicId) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class, clinicId);
            return value == null ? 0L : value;
        } catch (DataAccessException ex) {
            // Keep the system backward compatible when optional module tables are not present.
            log.warn("Usage query failed for clinic {}: {}", clinicId, ex.getMessage());
            return 0L;
        }
    }
}

