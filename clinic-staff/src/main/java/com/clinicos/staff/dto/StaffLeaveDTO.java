package com.clinicos.staff.dto;

import com.clinicos.staff.entity.StaffLeave.LeaveStatus;
import com.clinicos.staff.entity.StaffLeave.LeaveType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffLeaveDTO {
    private Long id;
    private Long staffId;
    private String staffName;

    @NotNull(message = "Leave type is required")
    private LeaveType leaveType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private String reason;
    private LeaveStatus status;
    private Long approvedById;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private long days;

    private LocalDateTime createdAt;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class LeaveApprovalRequest {
    private boolean approved;
    private String rejectionReason;
}

