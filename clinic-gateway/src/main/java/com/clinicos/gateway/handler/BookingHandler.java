package com.clinicos.gateway.handler;

import com.clinicos.common.dto.BookingRequestDto;
import com.clinicos.common.entity.AuditEvent;
import com.clinicos.common.entity.ClinicProfile;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.service.AuditService;
import com.clinicos.common.service.BookingService;
import com.clinicos.common.service.ClinicProfileService;
import com.clinicos.gateway.booking.DoctorDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/** Public booking page endpoints, and the front desk's list of online requests. */
@Component
@RequiredArgsConstructor
public class BookingHandler {

    private final BookingService bookings;
    private final ClinicProfileService profiles;
    private final DoctorDirectory doctors;
    private final com.clinicos.gateway.booking.SlotFinder slots;
    private final ClinicAccess clinicAccess;
    private final AuditService audit;

    public Mono<ServerResponse> publicDoctors(ServerRequest request) {
        return Mono.fromCallable(() -> {
            ClinicProfile clinic = profiles.bookableBySlug(request.pathVariable("slug"));
            return doctors.doctors(clinic.getClinicId());
        }).flatMap(ClinicHandler::ok).onErrorResume(ClinicHandler::error);
    }

    /** GET /api/v1/public/clinics/{slug}/slots?date=2026-11-07&doctorId=3 - free times, no patient details. */
    public Mono<ServerResponse> publicSlots(ServerRequest request) {
        return Mono.fromCallable(() -> {
            ClinicProfile clinic = profiles.bookableBySlug(request.pathVariable("slug"));
            java.time.LocalDate date;
            try {
                date = java.time.LocalDate.parse(request.queryParam("date").orElse(""));
            } catch (java.time.format.DateTimeParseException e) {
                throw new RegistrationException(HttpStatus.BAD_REQUEST, "Choose a date.");
            }
            java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Kolkata"));
            if (date.isBefore(today) || date.isAfter(today.plusDays(60))) {
                throw new RegistrationException(HttpStatus.BAD_REQUEST, "Choose a date within the next 60 days.");
            }
            Long doctorId = request.queryParam("doctorId").filter(v -> !v.isBlank()).map(Long::valueOf).orElse(null);
            return slots.find(clinic.getClinicId(), date, doctorId);
        }).flatMap(ClinicHandler::ok).onErrorResume(e -> ClinicHandler.error(e instanceof NumberFormatException
                ? new RegistrationException(HttpStatus.BAD_REQUEST, "Invalid doctor.") : e));
    }

    public Mono<ServerResponse> publicRequest(ServerRequest request) {
        String slug = request.pathVariable("slug");
        String ip = clientIp(request);
        return request.bodyToMono(BookingRequestDto.class)
                .switchIfEmpty(Mono.error(new RegistrationException(HttpStatus.BAD_REQUEST, "Please fill in the form.")))
                .publishOn(Schedulers.boundedElastic())
                .map(body -> {
                    ClinicProfile clinic = profiles.bookableBySlug(slug);
                    return bookings.request(slug, body, ip, body.getDoctorId() == null ? java.util.List.of() : doctors.doctors(clinic.getClinicId()));
                })
                .flatMap(ClinicHandler::ok)
                .onErrorResume(ClinicHandler::error);
    }

    public Mono<ServerResponse> list(ServerRequest request) {
        return Mono.fromCallable(() -> {
            ClinicUser member = clinicAccess.member(request);
            return bookings.list(member.getClinicId(), request.queryParam("status").orElse("NEW"),
                    Integer.parseInt(request.queryParam("page").orElse("0")), Integer.parseInt(request.queryParam("size").orElse("50")));
        }).flatMap(ClinicHandler::ok).onErrorResume(e -> ClinicHandler.error(e instanceof NumberFormatException
                ? new RegistrationException(HttpStatus.BAD_REQUEST, "page and size must be numbers.") : e));
    }

    public Mono<ServerResponse> countNew(ServerRequest request) {
        return Mono.fromCallable(() -> Map.of("count", bookings.countNew(clinicAccess.member(request).getClinicId())))
                .flatMap(ClinicHandler::ok).onErrorResume(ClinicHandler::error);
    }

    @SuppressWarnings("unchecked")
    public Mono<ServerResponse> update(ServerRequest request) {
        return request.bodyToMono(Map.class)
                .switchIfEmpty(Mono.error(new RegistrationException(HttpStatus.BAD_REQUEST, "Status is required.")))
                .publishOn(Schedulers.boundedElastic())
                .map(body -> {
                    ClinicUser member = clinicAccess.member(request);
                    Map<String, Object> b = (Map<String, Object>) body;
                    Long id = Long.valueOf(request.pathVariable("id"));
                    Object appointment = b.get("appointmentId");
                    BookingRequestDto updated = bookings.update(member.getClinicId(), id, String.valueOf(b.get("status")),
                            appointment == null ? null : Long.valueOf(String.valueOf(appointment)), member.getEmail());
                    audit.record(AuditEvent.builder().action("UPDATE").resourceType("booking-request").resourceId(String.valueOf(id))
                            .detail(updated.getStatus().toLowerCase()).httpMethod("PATCH").status(200).build(),
                            member.getClinicId(), member.getEmail(), clientIp(request));
                    return updated;
                })
                .flatMap(ClinicHandler::ok)
                .onErrorResume(e -> ClinicHandler.error(e instanceof NumberFormatException
                        ? new RegistrationException(HttpStatus.BAD_REQUEST, "Invalid request number.") : e));
    }

    private static String clientIp(ServerRequest request) {
        String forwarded = request.headers().firstHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) return forwarded.split(",")[0].trim();
        return request.remoteAddress().map(a -> a.getAddress() == null ? null : a.getAddress().getHostAddress()).orElse(null);
    }
}
