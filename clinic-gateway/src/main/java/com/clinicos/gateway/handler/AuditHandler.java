package com.clinicos.gateway.handler;

import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/** Clinic admins read the audit log; any member's browser reports exports and prints. */
@Component
@RequiredArgsConstructor
public class AuditHandler {

    private final ClinicAccess clinicAccess;
    private final AuditService audit;

    public Mono<ServerResponse> search(ServerRequest request) {
        return Mono.fromCallable(() -> {
            ClinicUser admin = clinicAccess.admin(request);
            return audit.search(admin.getClinicId(), param(request, "actor"), param(request, "resourceType"), param(request, "resourceId"),
                    date(request, "from"), date(request, "to"), integer(request, "page", 0), integer(request, "size", 50));
        }).flatMap(ClinicHandler::ok).onErrorResume(ClinicHandler::error);
    }

    @SuppressWarnings("unchecked")
    public Mono<ServerResponse> reportClientEvent(ServerRequest request) {
        return request.bodyToMono(Map.class)
                .switchIfEmpty(Mono.error(new RegistrationException(HttpStatus.BAD_REQUEST, "Event details are required.")))
                .publishOn(Schedulers.boundedElastic())
                .map(body -> {
                    ClinicUser member = clinicAccess.member(request);
                    Map<String, Object> b = (Map<String, Object>) body;
                    audit.recordClient(member.getClinicId(), member.getEmail(),
                            request.headers().firstHeader("X-Forwarded-For"),
                            str(b.get("action")), str(b.get("resourceType")), str(b.get("resourceId")), str(b.get("detail")));
                    return "recorded";
                })
                .flatMap(ClinicHandler::ok)
                .onErrorResume(ClinicHandler::error);
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String param(ServerRequest request, String name) {
        return request.queryParam(name).filter(v -> !v.isBlank()).orElse(null);
    }

    private static LocalDate date(ServerRequest request, String name) {
        String value = param(request, name);
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new RegistrationException(HttpStatus.BAD_REQUEST, "Dates must look like 2026-10-29.");
        }
    }

    private static int integer(ServerRequest request, String name, int fallback) {
        String value = param(request, name);
        try {
            return value == null ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new RegistrationException(HttpStatus.BAD_REQUEST, name + " must be a number.");
        }
    }
}
