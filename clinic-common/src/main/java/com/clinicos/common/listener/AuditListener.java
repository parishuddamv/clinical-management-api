package com.clinicos.common.listener;

import com.clinicos.common.entity.BaseEntity;
import com.clinicos.common.entity.TenantContext;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.stereotype.Component;

/**
 * JPA lifecycle listener for automatic audit trail and tenant assignment.
 * Ensures all entities are properly marked with clinic_id and timestamps.
 */
@Component
public class AuditListener {

    @PrePersist
    public void prePersist(BaseEntity entity) {
        if (entity.getClinicId() == null) {
            entity.setClinicId(TenantContext.getClinicId());
        }
    }

    @PreUpdate
    public void preUpdate(BaseEntity entity) {
        // Timestamp update is handled by @UpdateTimestamp in BaseEntity
    }
}

