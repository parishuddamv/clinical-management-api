package com.clinicos.common.entity;

/**
 * ThreadLocal-based context for storing the current tenant (clinic) ID.
 * This is populated by the TenantFilter and used for automatic clinic isolation.
 */
public class TenantContext {

    private static final ThreadLocal<String> clinicIdHolder = new ThreadLocal<>();

    public static void setClinicId(String clinicId) {
        clinicIdHolder.set(clinicId);
    }

    public static String getClinicId() {
        return clinicIdHolder.get();
    }

    public static void clear() {
        clinicIdHolder.remove();
    }
}

