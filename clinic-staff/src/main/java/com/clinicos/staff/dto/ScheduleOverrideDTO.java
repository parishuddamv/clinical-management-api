package com.clinicos.staff.dto;

import com.clinicos.staff.entity.ScheduleOverride.OverrideType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Schedule override (holiday, leave, special hours, blocked day) for a staff member.
 * Field names match the cms-ui ScheduleOverride / CreateScheduleOverrideRequest types.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleOverrideDTO {
    private Long id;
    private Long staffId;

    @NotNull(message = "Override date is required")
    private LocalDate overrideDate;

    @NotNull(message = "Override type is required")
    private OverrideType overrideType;

    private String reason;
    private LocalTime alternateStartTime;
    private LocalTime alternateEndTime;
    private Boolean isFullDayOff;
}
