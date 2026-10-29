package com.clinicos.gateway.booking;

import com.clinicos.common.dto.PublicDoctorDto;
import com.clinicos.common.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Active doctors of a clinic for its public booking page: name and speciality only. Asks the staff
 * service with a short-lived internal token, and keeps the answer for five minutes.
 */
@Slf4j
@Component
public class DoctorDirectory {

    private static final Duration CACHE = Duration.ofMinutes(5);
    private static final String SERVICE_USER = "online-booking@system.clinisysone";

    private final WebClient staff;
    private final JwtTokenProvider tokens;
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    public DoctorDirectory(@Value("${service.staff.url:http://localhost:8087}") String staffUrl, JwtTokenProvider tokens) {
        this.staff = WebClient.builder().baseUrl(staffUrl).build();
        this.tokens = tokens;
    }

    private record Cached(List<PublicDoctorDto> doctors, long at) { }

    /** Blocking; call off the event loop. Never throws: an unreachable staff service means no doctor list. */
    public List<PublicDoctorDto> doctors(String clinicId) {
        Cached hit = cache.get(clinicId);
        if (hit != null && System.currentTimeMillis() - hit.at() < CACHE.toMillis()) return hit.doctors();
        try {
            JsonNode body = staff.get().uri("/api/v1/staff/doctors")
                    .header("Authorization", "Bearer " + tokens.generateToken(SERVICE_USER, clinicId))
                    .retrieve().bodyToMono(JsonNode.class).block(Duration.ofSeconds(5));
            List<PublicDoctorDto> list = new ArrayList<>();
            JsonNode data = body == null ? null : body.path("data");
            if (data != null && data.isArray()) {
                for (JsonNode d : data) {
                    String name = (d.path("firstName").asText("") + " " + d.path("lastName").asText("")).trim().replaceFirst("(?i)^dr\\.?\\s+", "");
                    if (name.isEmpty() || !d.hasNonNull("id")) continue;
                    list.add(PublicDoctorDto.builder().id(d.path("id").asLong()).name("Dr. " + name)
                            .specialization(d.hasNonNull("specialization") ? d.path("specialization").asText() : null).build());
                }
            }
            cache.put(clinicId, new Cached(List.copyOf(list), System.currentTimeMillis()));
            return list;
        } catch (RuntimeException e) {
            log.warn("Doctor list unavailable for clinic {}: {}", clinicId, e.getClass().getSimpleName());
            return hit != null ? hit.doctors() : List.of();
        }
    }
}
