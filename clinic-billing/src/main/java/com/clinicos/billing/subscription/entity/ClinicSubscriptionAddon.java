package com.clinicos.billing.subscription.entity;

import com.clinicos.billing.subscription.model.AddonCode;
import com.clinicos.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "clinic_subscription_addons", uniqueConstraints = {
        @UniqueConstraint(name = "uk_clinic_addon", columnNames = {"clinic_id", "addon_code"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicSubscriptionAddon extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "addon_code", nullable = false, length = 40)
    private AddonCode addonCode;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}

