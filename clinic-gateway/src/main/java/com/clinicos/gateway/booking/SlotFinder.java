package com.clinicos.gateway.booking;

import com.clinicos.common.dto.PublicDoctorDto;
import com.clinicos.common.dto.PublicSlotsDto;
import com.clinicos.common.security.JwtTokenProvider;
import com.clinicos.common.service.OpenSlots;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Free times for the public booking page: each doctor's hours (staff service) minus their
 * appointments that day (appointment service). Uses a short-lived internal token, caches for a minute.
 */
@Slf4j
@Component
public class SlotFinder {

    private static final String SERVICE_USER = "online-booking@system.clinisysone";
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Kolkata");
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final long CACHE_MS = 60_000;

    private final WebClient staff;
    private final WebClient appointments;
    private final JwtTokenProvider tokens;
    private final DoctorDirectory directory;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final Map<String, Hours> hasHours = new ConcurrentHashMap<>();

    /** Whether a doctor has weekly hours, re-checked every five minutes so new hours show up. */
    private record Hours(boolean set, long at) { }

    private record Cached(PublicSlotsDto value, long at) { }

    public SlotFinder(@Value("${service.staff.url:http://localhost:8087}") String staffUrl,
                      @Value("${service.appointment.url:http://localhost:8082}") String appointmentUrl,
                      JwtTokenProvider tokens, DoctorDirectory directory) {
        this.staff = WebClient.builder().baseUrl(staffUrl).build();
        this.appointments = WebClient.builder().baseUrl(appointmentUrl).build();
        this.tokens = tokens;
        this.directory = directory;
    }

    /** Blocking; call off the event loop. doctorId null = any doctor. */
    public PublicSlotsDto find(String clinicId, LocalDate date, Long doctorId) {
        String key = clinicId + "|" + date + "|" + doctorId;
        Cached hit = cache.get(key);
        if (hit != null && System.currentTimeMillis() - hit.at() < CACHE_MS) return hit.value();

        List<PublicDoctorDto> doctors = directory.doctors(clinicId);
        List<Long> ids = doctors.stream().map(PublicDoctorDto::getId)
                .filter(id -> doctorId == null || id.equals(doctorId)).toList();
        String auth = "Bearer " + tokens.generateToken(SERVICE_USER, clinicId);
        List<OpenSlots.DoctorDay> days = new ArrayList<>();
        try {
            for (Long id : ids) days.add(day(clinicId, id, date, auth));
        } catch (RuntimeException e) {
            log.warn("Free times unavailable for clinic {}: {}", clinicId, e.getClass().getSimpleName());
            // Fall back to morning/afternoon/evening rather than show wrong times.
            return PublicSlotsDto.builder().date(date.toString()).exact(false).slots(List.of()).build();
        }
        PublicSlotsDto result = OpenSlots.compute(date, days, LocalDateTime.now(CLINIC_ZONE));
        cache.put(key, new Cached(result, System.currentTimeMillis()));
        return result;
    }

    private OpenSlots.DoctorDay day(String clinicId, Long doctorId, LocalDate date, String auth) {
        String hoursKey = clinicId + "|" + doctorId;
        Hours known = hasHours.get(hoursKey);
        if (known == null || System.currentTimeMillis() - known.at() > 5 * CACHE_MS) {
            JsonNode schedule = get(staff, "/api/v1/staff/schedules/staff/" + doctorId, auth);
            known = new Hours(schedule.path("data").isArray() && !schedule.path("data").isEmpty(), System.currentTimeMillis());
            hasHours.put(hoursKey, known);
        }
        boolean hours = known.set();
        if (!hours) return new OpenSlots.DoctorDay(false, List.of(), 15, List.of());

        JsonNode availability = get(staff, "/api/v1/staff/schedules/staff/" + doctorId + "/availability?date=" + date, auth).path("data");
        List<LocalTime> slots = new ArrayList<>();
        int step = 15;
        if (availability.path("isWorking").asBoolean(false)) {
            for (JsonNode s : availability.path("availableSlots")) {
                LocalTime start = LocalTime.parse(s.path("startTime").asText());
                slots.add(start);
                if (s.hasNonNull("endTime")) step = (int) Duration.between(start, LocalTime.parse(s.path("endTime").asText())).toMinutes();
            }
        }
        List<LocalTime> booked = new ArrayList<>();
        if (!slots.isEmpty()) {
            for (JsonNode a : get(appointments, "/api/v1/appointments/day?date=" + date + "&doctorId=" + doctorId, auth).path("data")) {
                String at = a.path("appointmentDateTime").asText("");
                if (at.length() >= 16) booked.add(LocalTime.parse(at.substring(11, 16)));
            }
        }
        return new OpenSlots.DoctorDay(true, slots, step, booked);
    }

    private static JsonNode get(WebClient client, String uri, String auth) {
        JsonNode body = client.get().uri(uri).header("Authorization", auth).retrieve().bodyToMono(JsonNode.class).block(TIMEOUT);
        if (body == null) throw new IllegalStateException("Empty response from " + uri);
        return body;
    }
}
