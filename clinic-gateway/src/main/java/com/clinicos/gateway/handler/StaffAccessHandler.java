package com.clinicos.gateway.handler;

import com.clinicos.common.dto.StaffAccessDto;
import com.clinicos.common.dto.StaffInviteRequest;
import com.clinicos.common.entity.AuditEvent;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.security.RolePolicy;
import com.clinicos.common.service.AuditService;
import com.clinicos.common.service.ClinicProfileService;
import com.clinicos.common.service.StaffAccessService;
import com.clinicos.gateway.security.RoleAccessFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/** Who am I (pages I can use), and the owner's management of staff sign-in access. */
@Component
@RequiredArgsConstructor
public class StaffAccessHandler {

    private final ClinicAccess clinicAccess;
    private final StaffAccessService staff;
    private final ClinicProfileService profiles;
    private final AuditService audit;
    private final RoleAccessFilter roleFilter;

    public Mono<ServerResponse> me(ServerRequest request) {
        return Mono.fromCallable(() -> staff.me(clinicAccess.member(request)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(ClinicHandler::ok).onErrorResume(ClinicHandler::error);
    }

    public Mono<ServerResponse> list(ServerRequest request) {
        return Mono.fromCallable(() -> staff.list(clinicAccess.require(request, RolePolicy.Page.STAFF).getClinicId()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(ClinicHandler::ok).onErrorResume(ClinicHandler::error);
    }

    public Mono<ServerResponse> invite(ServerRequest request) {
        return request.bodyToMono(StaffInviteRequest.class)
                .switchIfEmpty(Mono.error(new RegistrationException(HttpStatus.BAD_REQUEST, "Staff details are required.")))
                .publishOn(Schedulers.boundedElastic())
                .map(body -> {
                    ClinicUser actor = clinicAccess.require(request, RolePolicy.Page.STAFF);
                    StaffAccessDto result = staff.invite(actor, body, clinicName(actor));
                    roleFilter.forget(result.getEmail());
                    record(actor, "INVITE", result, request);
                    return result;
                })
                .flatMap(ClinicHandler::ok).onErrorResume(ClinicHandler::error);
    }

    @SuppressWarnings("unchecked")
    public Mono<ServerResponse> resend(ServerRequest request) {
        return request.bodyToMono(Map.class).defaultIfEmpty(Map.of())
                .publishOn(Schedulers.boundedElastic())
                .map(body -> {
                    ClinicUser actor = clinicAccess.require(request, RolePolicy.Page.STAFF);
                    StaffAccessDto result = staff.resend(actor, String.valueOf(((Map<String, Object>) body).get("email")), clinicName(actor));
                    record(actor, "RESEND_INVITE", result, request);
                    return result;
                })
                .flatMap(ClinicHandler::ok).onErrorResume(ClinicHandler::error);
    }

    @SuppressWarnings("unchecked")
    public Mono<ServerResponse> revoke(ServerRequest request) {
        return request.bodyToMono(Map.class).defaultIfEmpty(Map.of())
                .publishOn(Schedulers.boundedElastic())
                .map(body -> {
                    ClinicUser actor = clinicAccess.require(request, RolePolicy.Page.STAFF);
                    StaffAccessDto result = staff.revoke(actor, String.valueOf(((Map<String, Object>) body).get("email")));
                    roleFilter.forget(result.getEmail());
                    record(actor, "REVOKE_ACCESS", result, request);
                    return result;
                })
                .flatMap(ClinicHandler::ok).onErrorResume(ClinicHandler::error);
    }

    private String clinicName(ClinicUser actor) {
        try {
            return profiles.get(actor.getClinicId()).getDisplayName();
        } catch (RuntimeException e) {
            return actor.getClinicName();
        }
    }

    private void record(ClinicUser actor, String action, StaffAccessDto target, ServerRequest request) {
        try {
            audit.record(AuditEvent.builder().action(action).resourceType("staff-access").resourceId(target.getEmail())
                            .detail(target.getRoleLabel()).httpMethod(request.methodName()).path(request.path()).status(200).build(),
                    actor.getClinicId(), actor.getEmail(), request.headers().firstHeader("X-Forwarded-For"));
        } catch (RuntimeException ignored) {
            // Auditing never blocks the action
        }
    }
}
