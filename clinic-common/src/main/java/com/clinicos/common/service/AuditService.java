package com.clinicos.common.service;

import com.clinicos.common.dto.AuditEventDto;
import com.clinicos.common.entity.AuditEvent;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;

/** Stores and searches the clinic audit log. Recording never fails the user's request. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    /** Actions the web app may report for things that happen in the browser (exports, printing). */
    public static final Set<String> CLIENT_ACTIONS = Set.of("EXPORT", "PRINT", "SHARE");

    private final ObjectProvider<AuditEventRepository> events;

    public void record(AuditEvent event, String clinicId, String actor, String ip) {
        if (event == null || clinicId == null || actor == null) return;
        try {
            event.setClinicId(clinicId);
            event.setActor(actor.length() > 100 ? actor.substring(0, 100) : actor.toLowerCase(Locale.ROOT));
            event.setIpAddress(ip == null ? null : ip.substring(0, Math.min(64, ip.length())));
            if (event.getOccurredAt() == null) event.setOccurredAt(LocalDateTime.now());
            events.getObject().save(event);
        } catch (RuntimeException e) {
            log.warn("Audit event not stored ({} {}): {}", event.getAction(), event.getResourceType(), e.getClass().getSimpleName());
        }
    }

    /** An action reported by the web app, e.g. a CSV export or a print. */
    public void recordClient(String clinicId, String actor, String ip, String action, String resourceType, String resourceId, String detail) {
        String a = action == null ? "" : action.trim().toUpperCase(Locale.ROOT);
        if (!CLIENT_ACTIONS.contains(a)) throw new RegistrationException(HttpStatus.BAD_REQUEST, "Unsupported audit action.");
        String type = resourceType == null ? "" : resourceType.trim().toLowerCase(Locale.ROOT);
        if (!type.matches("[a-z][a-z-]{1,39}")) throw new RegistrationException(HttpStatus.BAD_REQUEST, "Unsupported record type.");
        String id = resourceId == null || resourceId.isBlank() ? null : resourceId.trim();
        if (id != null && !id.matches("[A-Za-z0-9-]{1,40}")) throw new RegistrationException(HttpStatus.BAD_REQUEST, "Invalid record id.");
        String d = detail == null ? null : detail.trim().replaceAll("[^A-Za-z0-9 _-]", "");
        record(AuditEvent.builder().action(a).resourceType(type).resourceId(id).detail(d == null || d.isEmpty() ? null : d.substring(0, Math.min(60, d.length())))
                .httpMethod("CLIENT").status(200).build(), clinicId, actor, ip);
    }

    public Page<AuditEventDto> search(String clinicId, String actor, String resourceType, String resourceId, LocalDate from, LocalDate to, int page, int size) {
        if (page < 0 || size < 1 || size > 200) throw new RegistrationException(HttpStatus.BAD_REQUEST, "page must be 0 or more and size 1-200.");
        Specification<AuditEvent> spec = (root, q, cb) -> cb.equal(root.get("clinicId"), clinicId);
        if (actor != null && !actor.isBlank()) spec = spec.and((root, q, cb) -> cb.equal(root.get("actor"), actor.trim().toLowerCase(Locale.ROOT)));
        if (resourceType != null && !resourceType.isBlank()) spec = spec.and((root, q, cb) -> cb.equal(root.get("resourceType"), resourceType.trim().toLowerCase(Locale.ROOT)));
        if (resourceId != null && !resourceId.isBlank()) spec = spec.and((root, q, cb) -> cb.equal(root.get("resourceId"), resourceId.trim()));
        if (from != null) spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("occurredAt"), from.atStartOfDay()));
        if (to != null) spec = spec.and((root, q, cb) -> cb.lessThan(root.get("occurredAt"), to.plusDays(1).atStartOfDay()));
        return events.getObject().findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt", "id"))).map(AuditEventDto::from);
    }
}
