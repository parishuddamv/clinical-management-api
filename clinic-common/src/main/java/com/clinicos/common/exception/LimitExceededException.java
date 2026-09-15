package com.clinicos.common.exception;

import java.util.Map;
import java.util.HashMap;

/**
 * Raised when a plan usage quota is exhausted.
 */
public class LimitExceededException extends ClinicOSException {

    private final String limitKey;
    private final long currentUsage;
    private final Integer allowedMaximum;
    private final String currentPlan;
    private final String upgradeRecommendation;

    public LimitExceededException(
            String limitKey,
            long currentUsage,
            Integer allowedMaximum,
            String currentPlan,
            String upgradeRecommendation,
            String message
    ) {
        super("LIMIT_EXCEEDED", message);
        this.limitKey = limitKey;
        this.currentUsage = currentUsage;
        this.allowedMaximum = allowedMaximum;
        this.currentPlan = currentPlan;
        this.upgradeRecommendation = upgradeRecommendation;
    }

    public String getLimitKey() {
        return limitKey;
    }

    public long getCurrentUsage() {
        return currentUsage;
    }

    public Integer getAllowedMaximum() {
        return allowedMaximum;
    }

    public String getCurrentPlan() {
        return currentPlan;
    }

    public String getUpgradeRecommendation() {
        return upgradeRecommendation;
    }

    public Map<String, Object> toResponseData() {
        Map<String, Object> data = new HashMap<>();
        data.put("limitKey", limitKey);
        data.put("currentUsage", currentUsage);
        data.put("allowedMaximum", allowedMaximum);
        data.put("currentPlan", currentPlan);
        data.put("upgradeRecommendation", upgradeRecommendation);
        return data;
    }
}

