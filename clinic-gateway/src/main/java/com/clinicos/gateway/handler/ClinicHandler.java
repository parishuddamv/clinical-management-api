package com.clinicos.gateway.handler;

import com.clinicos.common.dto.ApiResponse;
import com.clinicos.common.dto.ClinicProfileDto;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.service.ClinicProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/** Clinic profile (letterhead, GSTIN, UPI, booking settings) and the public clinic lookup for booking pages. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClinicHandler {

    private final ClinicAccess clinicAccess;
    private final ClinicProfileService profiles;
    private final com.clinicos.common.service.AuditService audit;

    public Mono<ServerResponse> getProfile(ServerRequest request) {
        return Mono.fromCallable(() -> profiles.get(clinicAccess.member(request).getClinicId()))
                .flatMap(ClinicHandler::ok)
                .onErrorResume(ClinicHandler::error);
    }

    public Mono<ServerResponse> saveProfile(ServerRequest request) {
        return request.bodyToMono(ClinicProfileDto.class)
                .switchIfEmpty(Mono.error(new RegistrationException(HttpStatus.BAD_REQUEST, "Profile details are required.")))
                .publishOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .map(body -> {
                    ClinicUser admin = clinicAccess.admin(request);
                    ClinicProfileDto saved = profiles.save(admin.getClinicId(), body, admin.getEmail());
                    audit.record(com.clinicos.common.entity.AuditEvent.builder().action("UPDATE").resourceType("clinic-profile")
                            .httpMethod("PUT").path("/api/v1/clinic/profile").status(200).build(),
                            admin.getClinicId(), admin.getEmail(), request.headers().firstHeader("X-Forwarded-For"));
                    return saved;
                })
                .flatMap(ClinicHandler::ok)
                .onErrorResume(ClinicHandler::error);
    }

    public Mono<ServerResponse> publicClinic(ServerRequest request) {
        return Mono.fromCallable(() -> profiles.publicBySlug(request.pathVariable("slug")))
                .flatMap(ClinicHandler::ok)
                .onErrorResume(ClinicHandler::error);
    }

    static Mono<ServerResponse> ok(Object body) {
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(ApiResponse.success(body));
    }

    static Mono<ServerResponse> error(Throwable e) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Request could not be processed. Please try again later.";
        if (e instanceof RegistrationException registration) {
            status = registration.getStatus();
            message = registration.getMessage();
        } else if (e instanceof org.springframework.web.server.ServerWebInputException
                || e instanceof org.springframework.core.codec.DecodingException) {
            status = HttpStatus.BAD_REQUEST;
            message = "Invalid request body.";
        } else {
            log.error("Clinic request failed: {}", e.getMessage(), e);
        }
        return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).bodyValue(ApiResponse.error(message));
    }
}
