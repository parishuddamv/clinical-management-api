package com.clinicos.emr.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** A clinic's dental procedure price list, stored as JSON. One row per clinic. */
@Entity
@Table(name = "dental_price_lists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DentalPriceList {

    @Id
    @Column(name = "clinic_id", nullable = false, length = 50)
    private String clinicId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String data;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
