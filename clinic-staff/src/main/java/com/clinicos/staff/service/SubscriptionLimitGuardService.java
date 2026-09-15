package com.clinicos.staff.service;

import com.clinicos.common.exception.LimitExceededException;
import com.clinicos.staff.entity.StaffMember;
import com.clinicos.staff.repository.StaffMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionLimitGuardService {

    private final JdbcTemplate jdbcTemplate;
    private final StaffMemberRepository staffMemberRepository;

    public void validateCreateStaffWithinPlan(String clinicId, StaffMember.StaffRole roleToCreate) {
        enforceLimit(clinicId, "STAFF");
        if (roleToCreate == StaffMember.StaffRole.DOCTOR) {
            enforceLimit(clinicId, "DOCTORS");
        }
    }

    private void enforceLimit(String clinicId, String limitKey) {
        PlanLimitSnapshot snapshot = fetchPlanLimit(clinicId, limitKey);
        if (snapshot == null || snapshot.maxValue() == null) {
            return;
        }

        long usage = switch (limitKey) {
            case "DOCTORS" -> staffMemberRepository.countByClinicIdAndRole(clinicId, StaffMember.StaffRole.DOCTOR);
            case "STAFF" -> staffMemberRepository.countByClinicIdAndStatus(clinicId, StaffMember.StaffStatus.ACTIVE);
            default -> 0L;
        };

        if (usage >= snapshot.maxValue()) {
            throw new LimitExceededException(
                    limitKey,
                    usage,
                    snapshot.maxValue(),
                    snapshot.planCode(),
                    recommendUpgrade(snapshot.planCode()),
                    "Plan limit reached for " + limitKey.toLowerCase(Locale.ROOT)
            );
        }
    }

    private PlanLimitSnapshot fetchPlanLimit(String clinicId, String limitKey) {
        try {
            return jdbcTemplate.query(
                    "SELECT cs.plan_code, pl.max_value " +
                            "FROM clinic_subscriptions cs " +
                            "JOIN subscription_plans sp ON sp.code = cs.plan_code " +
                            "JOIN plan_limits pl ON pl.plan_id = sp.id " +
                            "WHERE cs.clinic_id = ? AND pl.limit_key = ?",
                    rs -> {
                        if (!rs.next()) {
                            return null;
                        }
                        return new PlanLimitSnapshot(rs.getString("plan_code"), (Integer) rs.getObject("max_value"));
                    },
                    clinicId,
                    limitKey
            );
        } catch (DataAccessException ex) {
            // Allow existing installs to continue if subscription tables are not present yet.
            log.warn("Skipping limit check for clinic {} key {} because query failed: {}", clinicId, limitKey, ex.getMessage());
            return null;
        }
    }

    private String recommendUpgrade(String planCode) {
        return switch (planCode) {
            case "STARTER" -> "PROFESSIONAL";
            case "PROFESSIONAL" -> "BUSINESS";
            case "BUSINESS" -> "ENTERPRISE";
            default -> "Contact support";
        };
    }

    private record PlanLimitSnapshot(String planCode, Integer maxValue) {
    }
}

