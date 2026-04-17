package com.clinicos.emr.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Drug Master entity - catalog of drugs for auto-suggestions.
 */
@Entity
@Table(name = "drugs_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DrugMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", length = 50)
    private String clinicId;

    @Column(name = "drug_code", length = 50)
    private String drugCode;

    @Column(name = "brand_name", nullable = false, length = 200)
    private String brandName;

    @Column(name = "generic_name", nullable = false, length = 200)
    private String genericName;

    @Column(name = "strength", length = 100)
    private String strength;

    @Enumerated(EnumType.STRING)
    @Column(name = "form", nullable = false, length = 50)
    private DrugForm form;

    @Column(name = "manufacturer", length = 200)
    private String manufacturer;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "schedule_type", length = 20)
    private String scheduleType;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum DrugForm {
        TABLET,
        CAPSULE,
        SYRUP,
        SUSPENSION,
        INJECTION,
        CREAM,
        OINTMENT,
        GEL,
        DROPS,
        INHALER,
        POWDER,
        SOFTGEL,
        SACHET,
        SPRAY,
        PATCH,
        SUPPOSITORY,
        LOTION,
        SOLUTION,
        OTHER
    }
}

