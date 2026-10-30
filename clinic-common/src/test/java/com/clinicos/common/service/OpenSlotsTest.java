package com.clinicos.common.service;

import com.clinicos.common.dto.PublicSlotsDto;
import com.clinicos.common.service.OpenSlots.DoctorDay;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenSlotsTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 7);
    private static final LocalDateTime EARLIER = LocalDateTime.of(2026, 11, 1, 10, 0);
    private static LocalTime t(String s) { return LocalTime.parse(s); }

    @Test
    void noDoctorHoursMeansPartOfDayRequests() {
        PublicSlotsDto r = OpenSlots.compute(DAY, List.of(new DoctorDay(false, List.of(), 15, List.of())), EARLIER);
        assertThat(r.isExact()).isFalse();
    }

    @Test
    void bookedTimesAreNotFree() {
        DoctorDay neha = new DoctorDay(true, List.of(t("10:00"), t("10:15"), t("10:30")), 15, List.of(t("10:05")));
        PublicSlotsDto r = OpenSlots.compute(DAY, List.of(neha), EARLIER);
        assertThat(r.getSlots()).extracting(PublicSlotsDto.Slot::getTime, PublicSlotsDto.Slot::isFree)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("10:00", false), org.assertj.core.groups.Tuple.tuple("10:15", true), org.assertj.core.groups.Tuple.tuple("10:30", true));
    }

    @Test
    void anyDoctorIsFreeIfOneOfThemIs() {
        DoctorDay a = new DoctorDay(true, List.of(t("10:00")), 15, List.of(t("10:00")));
        DoctorDay b = new DoctorDay(true, List.of(t("10:00"), t("18:00")), 20, List.of());
        PublicSlotsDto r = OpenSlots.compute(DAY, List.of(a, b), EARLIER);
        assertThat(r.getSlots()).extracting(PublicSlotsDto.Slot::getTime).containsExactly("10:00", "18:00");
        assertThat(r.getSlots()).allMatch(PublicSlotsDto.Slot::isFree);
    }

    @Test
    void timesTooSoonAreLeftOutAndAnEmptyDaySaysWhy() {
        DoctorDay d = new DoctorDay(true, List.of(t("10:00"), t("10:15"), t("11:00")), 15, List.of());
        PublicSlotsDto today = OpenSlots.compute(DAY, List.of(d), DAY.atTime(10, 20));
        assertThat(today.getSlots()).extracting(PublicSlotsDto.Slot::getTime).containsExactly("11:00");
        PublicSlotsDto late = OpenSlots.compute(DAY, List.of(d), DAY.atTime(20, 0));
        assertThat(late.getSlots()).isEmpty();
        assertThat(late.getReason()).isEqualTo("No more times today.");
        PublicSlotsDto off = OpenSlots.compute(DAY, List.of(new DoctorDay(true, List.of(), 15, List.of())), EARLIER);
        assertThat(off.getReason()).contains("isn't taking appointments");
    }
}
