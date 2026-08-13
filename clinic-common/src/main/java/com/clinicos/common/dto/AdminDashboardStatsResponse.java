package com.clinicos.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Admin Dashboard Statistics
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardStatsResponse {

    private Long totalNewUsers; // Users with status=NEW

    private Long totalPendingUsers; // Users with status=PENDING

    private Long totalApprovedUsers; // Users with status=APPROVED

    private Long totalRejectedUsers; // Users with status=REJECTED

    private Long totalSuspendedUsers; // Users with status=SUSPENDED

    private Long totalPendingDemos; // Demo bookings with status=PENDING

    private Long totalConfirmedDemos; // Demo bookings with status=CONFIRMED

    private Long totalCompletedDemos; // Demo bookings with status=COMPLETED

    private Long totalCancelledDemos; // Demo bookings with status=CANCELLED

    private Long totalActiveClinics; // Count of approved clinics

    private Long totalRegistrations; // Total users in system
}

