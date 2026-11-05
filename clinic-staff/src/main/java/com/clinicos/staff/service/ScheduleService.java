package com.clinicos.staff.service;

import com.clinicos.staff.dto.DoctorScheduleDTO;
import com.clinicos.staff.dto.ScheduleOverrideDTO;
import com.clinicos.staff.entity.DoctorSchedule;
import com.clinicos.staff.entity.ScheduleOverride;
import com.clinicos.staff.entity.StaffLeave;
import com.clinicos.staff.entity.StaffMember;
import com.clinicos.staff.repository.DoctorScheduleRepository;
import com.clinicos.staff.repository.ScheduleOverrideRepository;
import com.clinicos.staff.repository.StaffLeaveRepository;
import com.clinicos.staff.repository.StaffMemberRepository;
import com.clinicos.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final DoctorScheduleRepository scheduleRepository;
    private final ScheduleOverrideRepository overrideRepository;
    private final StaffLeaveRepository leaveRepository;
    private final StaffMemberRepository staffRepository;

    @Transactional
    public DoctorScheduleDTO createSchedule(String clinicId, DoctorScheduleDTO dto) {
        StaffMember staff = staffRepository.findByIdAndClinicId(dto.getStaffId(), clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + dto.getStaffId()));

        DoctorSchedule schedule = DoctorSchedule.builder()
                .staff(staff)
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .slotDurationMinutes(dto.getSlotDurationMinutes() != null ? dto.getSlotDurationMinutes() : 15)
                .maxPatientsPerSlot(dto.getMaxPatientsPerSlot() != null ? dto.getMaxPatientsPerSlot() : 1)
                .breakStart(dto.getBreakStart())
                .breakEnd(dto.getBreakEnd())
                .isActive(true)
                .effectiveFrom(dto.getEffectiveFrom())
                .effectiveUntil(dto.getEffectiveUntil())
                .build();

        schedule.setClinicId(clinicId);
        schedule = scheduleRepository.save(schedule);

        log.info("Created schedule for staff: {} on day: {}", dto.getStaffId(), dto.getDayOfWeek());
        return mapToDTO(schedule);
    }

    @Transactional
    public List<DoctorScheduleDTO> setWeeklySchedule(String clinicId, Long staffId, List<DoctorScheduleDTO> schedules) {
        StaffMember staff = staffRepository.findByIdAndClinicId(staffId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));

        // Deactivate existing schedules
        List<DoctorSchedule> existingSchedules = scheduleRepository.findByStaffIdAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(staffId);
        existingSchedules.forEach(s -> s.setIsActive(false));
        scheduleRepository.saveAll(existingSchedules);

        // Create new schedules
        List<DoctorSchedule> newSchedules = schedules.stream().map(dto -> {
            DoctorSchedule schedule = DoctorSchedule.builder()
                    .staff(staff)
                    .dayOfWeek(dto.getDayOfWeek())
                    .startTime(dto.getStartTime())
                    .endTime(dto.getEndTime())
                    .slotDurationMinutes(dto.getSlotDurationMinutes() != null ? dto.getSlotDurationMinutes() : 15)
                    .maxPatientsPerSlot(dto.getMaxPatientsPerSlot() != null ? dto.getMaxPatientsPerSlot() : 1)
                    .breakStart(dto.getBreakStart())
                    .breakEnd(dto.getBreakEnd())
                    .isActive(true)
                    .build();
            schedule.setClinicId(clinicId);
            return schedule;
        }).collect(Collectors.toList());

        newSchedules = scheduleRepository.saveAll(newSchedules);
        log.info("Set weekly schedule for staff: {} with {} slots", staffId, newSchedules.size());

        return newSchedules.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DoctorScheduleDTO> getStaffSchedule(String clinicId, Long staffId) {
        staffRepository.findByIdAndClinicId(staffId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));

        return scheduleRepository.findByStaffIdAndIsActiveTrueOrderByDayOfWeekAscStartTimeAsc(staffId)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDoctorAvailability(String clinicId, Long staffId, LocalDate date) {
        StaffMember staff = staffRepository.findByIdAndClinicId(staffId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));

        Map<String, Object> availability = new HashMap<>();
        availability.put("staffId", staffId);
        availability.put("staffName", staff.getFullName());
        availability.put("date", date);

        // Check for leave
        List<StaffLeave> leaves = leaveRepository.findApprovedLeavesForDate(staffId, date);
        if (!leaves.isEmpty()) {
            availability.put("isWorking", false);
            availability.put("reason", "On Leave: " + leaves.get(0).getLeaveType());
            availability.put("availableSlots", Collections.emptyList());
            return availability;
        }

        // Check for schedule override
        Optional<ScheduleOverride> override = overrideRepository.findByStaffIdAndOverrideDate(staffId, date);
        if (override.isPresent()) {
            ScheduleOverride o = override.get();
            if (o.getOverrideType() == ScheduleOverride.OverrideType.HOLIDAY ||
                o.getOverrideType() == ScheduleOverride.OverrideType.BLOCKED) {
                availability.put("isWorking", false);
                availability.put("reason", o.getOverrideType() + ": " + o.getReason());
                availability.put("availableSlots", Collections.emptyList());
                return availability;
            } else if (o.getOverrideType() == ScheduleOverride.OverrideType.SPECIAL_HOURS) {
                // Use override hours
                availability.put("isWorking", true);
                availability.put("availableSlots", generateTimeSlots(o.getStartTime(), o.getEndTime(), 15, null, null));
                return availability;
            }
        }

        // Get regular schedule for the day
        int dayOfWeek = date.getDayOfWeek().getValue() % 7; // Convert to 0=Sunday format
        List<DoctorSchedule> schedules = scheduleRepository.findActiveScheduleForDay(staffId, dayOfWeek, date);

        if (schedules.isEmpty()) {
            availability.put("isWorking", false);
            availability.put("reason", "Not scheduled for this day");
            availability.put("availableSlots", Collections.emptyList());
            return availability;
        }

        availability.put("isWorking", true);
        List<Map<String, Object>> allSlots = new ArrayList<>();
        for (DoctorSchedule schedule : schedules) {
            allSlots.addAll(generateTimeSlots(
                    schedule.getStartTime(),
                    schedule.getEndTime(),
                    schedule.getSlotDurationMinutes(),
                    schedule.getBreakStart(),
                    schedule.getBreakEnd()
            ));
        }
        availability.put("availableSlots", allSlots);

        return availability;
    }

    @Transactional
    public void addScheduleOverride(String clinicId, Long staffId, ScheduleOverride.OverrideType type,
                                    LocalDate date, LocalTime startTime, LocalTime endTime, String reason) {
        StaffMember staff = staffRepository.findByIdAndClinicId(staffId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));

        ScheduleOverride override = ScheduleOverride.builder()
                .staff(staff)
                .overrideDate(date)
                .overrideType(type)
                .startTime(startTime)
                .endTime(endTime)
                .reason(reason)
                .isFullDay(startTime == null && endTime == null)
                .build();

        override.setClinicId(clinicId);
        overrideRepository.save(override);
        log.info("Added schedule override for staff: {} on: {}", staffId, date);
    }

    @Transactional
    public ScheduleOverrideDTO addScheduleOverride(String clinicId, Long staffId, ScheduleOverrideDTO dto) {
        StaffMember staff = staffRepository.findByIdAndClinicId(staffId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));

        boolean specialHours = dto.getOverrideType() == ScheduleOverride.OverrideType.SPECIAL_HOURS;
        LocalTime start = specialHours ? dto.getAlternateStartTime() : null;
        LocalTime end = specialHours ? dto.getAlternateEndTime() : null;

        ScheduleOverride override = ScheduleOverride.builder()
                .staff(staff)
                .overrideDate(dto.getOverrideDate())
                .overrideType(dto.getOverrideType())
                .startTime(start)
                .endTime(end)
                .reason(dto.getReason())
                .isFullDay(start == null && end == null)
                .build();

        override.setClinicId(clinicId);
        ScheduleOverride saved = overrideRepository.save(override);
        log.info("Added schedule override for staff: {} on: {}", staffId, dto.getOverrideDate());
        return toOverrideDTO(saved, staffId);
    }

    /**
     * Lists overrides for a staff member. Returns an empty list (never null) when there are none.
     * Defaults to the last 30 days through the next 365 days when no range is given.
     */
    @Transactional(readOnly = true)
    public List<ScheduleOverrideDTO> getScheduleOverrides(String clinicId, Long staffId,
                                                          LocalDate startDate, LocalDate endDate) {
        staffRepository.findByIdAndClinicId(staffId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));

        LocalDate from = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate to = endDate != null ? endDate : LocalDate.now().plusDays(365);
        if (to.isBefore(from)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }

        return overrideRepository
                .findByStaffIdAndOverrideDateBetweenOrderByOverrideDateAsc(staffId, from, to)
                .stream()
                .filter(o -> clinicId.equals(o.getClinicId()))
                .map(o -> toOverrideDTO(o, staffId))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteScheduleOverride(String clinicId, Long overrideId) {
        ScheduleOverride override = overrideRepository.findByIdAndClinicId(overrideId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule override not found: " + overrideId));
        overrideRepository.delete(override);
        log.info("Deleted schedule override: {}", overrideId);
    }

    private ScheduleOverrideDTO toOverrideDTO(ScheduleOverride o, Long staffId) {
        return ScheduleOverrideDTO.builder()
                .id(o.getId())
                .staffId(staffId)
                .overrideDate(o.getOverrideDate())
                .overrideType(o.getOverrideType())
                .reason(o.getReason())
                .alternateStartTime(o.getStartTime())
                .alternateEndTime(o.getEndTime())
                .isFullDayOff(o.getIsFullDay() == null || o.getIsFullDay())
                .build();
    }

    private List<Map<String, Object>> generateTimeSlots(LocalTime start, LocalTime end, int durationMinutes,
                                                         LocalTime breakStart, LocalTime breakEnd) {
        List<Map<String, Object>> slots = new ArrayList<>();
        LocalTime current = start;

        while (current.plusMinutes(durationMinutes).compareTo(end) <= 0) {
            // Skip break time
            if (breakStart != null && breakEnd != null) {
                if (!current.isBefore(breakStart) && current.isBefore(breakEnd)) {
                    current = breakEnd;
                    continue;
                }
            }

            Map<String, Object> slot = new HashMap<>();
            slot.put("startTime", current.toString());
            slot.put("endTime", current.plusMinutes(durationMinutes).toString());
            slot.put("isAvailable", true);
            slots.add(slot);

            current = current.plusMinutes(durationMinutes);
        }

        return slots;
    }

    private DoctorScheduleDTO mapToDTO(DoctorSchedule schedule) {
        return DoctorScheduleDTO.builder()
                .id(schedule.getId())
                .staffId(schedule.getStaff().getId())
                .staffName(schedule.getStaff().getFullName())
                .dayOfWeek(schedule.getDayOfWeek())
                .dayName(schedule.getDayName())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .slotDurationMinutes(schedule.getSlotDurationMinutes())
                .maxPatientsPerSlot(schedule.getMaxPatientsPerSlot())
                .breakStart(schedule.getBreakStart())
                .breakEnd(schedule.getBreakEnd())
                .isActive(schedule.getIsActive())
                .effectiveFrom(schedule.getEffectiveFrom())
                .effectiveUntil(schedule.getEffectiveUntil())
                .build();
    }
}

