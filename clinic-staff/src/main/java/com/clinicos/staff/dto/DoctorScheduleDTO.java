package com.clinicos.staff.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorScheduleDTO {
    private Long id;
    private Long staffId;
    private String staffName;

    @NotNull(message = "Day of week is required")
    private Integer dayOfWeek;
    private String dayName;

    @NotNull(message = "Start time is required")
    private LocalTime startTime;

    @NotNull(message = "End time is required")
    private LocalTime endTime;

    private Integer slotDurationMinutes;
    private Integer maxPatientsPerSlot;
    private LocalTime breakStart;
    private LocalTime breakEnd;
    private Boolean isActive;
    private LocalDate effectiveFrom;
    private LocalDate effectiveUntil;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class WeeklyScheduleDTO {
    private Long staffId;
    private String staffName;
    private List<DoctorScheduleDTO> schedules;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class TimeSlotDTO {
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isAvailable;
    private int bookedCount;
    private int maxCapacity;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class DoctorAvailabilityDTO {
    private Long staffId;
    private String staffName;
    private LocalDate date;
    private boolean isWorking;
    private String reason; // If not working
    private List<TimeSlotDTO> availableSlots;
}

