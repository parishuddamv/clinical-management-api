package com.clinicos.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** One recorded action: who did what to which record, and whether it was allowed. */
@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "clinic_id", nullable = false, length = 50)
    private String clinicId;

    @Column(nullable = false, length = 100)
    private String actor;

    @Column(nullable = false, length = 30)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 40)
    private String resourceType;

    @Column(name = "resource_id", length = 40)
    private String resourceId;

    @Column(length = 60)
    private String detail;

    @Column(name = "http_method", length = 10)
    private String httpMethod;

    @Column(length = 300)
    private String path;

    private Integer status;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private LocalDateTime occurredAt = LocalDateTime.now();
}
