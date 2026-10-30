package com.clinicos.common.service;

import com.clinicos.common.dto.PublicSlotsDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Works out free booking times from each doctor's hours and existing appointments. No I/O. */
public final class OpenSlots {

    /** Patients can't book a time starting sooner than this. */
    static final int LEAD_MINUTES = 30;

    private OpenSlots() { }

    /**
     * One doctor's day: whether they have weekly hours at all, the slot start times offered that
     * day (empty when off), the slot length, and the start times of their existing appointments.
     */
    public record DoctorDay(boolean hasHours, List<LocalTime> slots, int stepMinutes, List<LocalTime> booked) { }

    public static PublicSlotsDto compute(LocalDate date, List<DoctorDay> doctors, LocalDateTime now) {
        boolean exact = doctors.stream().anyMatch(DoctorDay::hasHours);
        if (!exact) return PublicSlotsDto.builder().date(date.toString()).exact(false).slots(List.of()).build();

        // A time is free when at least one doctor offers it and is not booked then.
        Map<LocalTime, Boolean> times = new TreeMap<>();
        for (DoctorDay d : doctors) {
            int step = Math.max(5, d.stepMinutes());
            for (LocalTime t : d.slots()) {
                if (isTooSoon(date, t, now)) continue;
                LocalTime end = t.plusMinutes(step);
                boolean taken = d.booked().stream().anyMatch(b -> !b.isBefore(t) && b.isBefore(end));
                times.merge(t, !taken, Boolean::logicalOr);
            }
        }
        List<PublicSlotsDto.Slot> slots = new ArrayList<>();
        times.forEach((t, free) -> slots.add(new PublicSlotsDto.Slot(t.toString(), free)));
        String reason = slots.isEmpty()
                ? (date.equals(now.toLocalDate()) ? "No more times today." : "The clinic isn't taking appointments on this day.")
                : null;
        return PublicSlotsDto.builder().date(date.toString()).exact(true).slots(slots).reason(reason).build();
    }

    private static boolean isTooSoon(LocalDate date, LocalTime t, LocalDateTime now) {
        return date.atTime(t).isBefore(now.plusMinutes(LEAD_MINUTES));
    }
}
