package com.clinicos.common.service;

import com.clinicos.common.entity.AuditEvent;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Turns an API call into an audit event. Detail reads (GET /patients/12) count as VIEW; list and
 * search calls are not recorded, since they would drown out the record-level history.
 */
public final class AuditTrail {

    private static final Set<String> AUDITED = Set.of("patients", "appointments", "followups", "billing", "emr", "staff", "feedback", "notifications");
    private static final Pattern NUMBER = Pattern.compile("\\d{1,18}");
    private static final Pattern WORD = Pattern.compile("[a-z][a-z-]{1,30}");

    private AuditTrail() {
    }

    /** Builds the event for a call, or empty when the call is not one worth recording. */
    public static Optional<AuditEvent> describe(String method, String path, int status) {
        if (method == null || path == null || !path.startsWith("/api/v1/")) return Optional.empty();
        String[] all = path.substring("/api/v1/".length()).split("/");
        if (all.length == 0 || !AUDITED.contains(all[0])) return Optional.empty();
        String[] seg = "emr".equals(all[0]) && all.length > 1 ? java.util.Arrays.copyOfRange(all, 1, all.length) : all;
        String verb = method.toUpperCase(Locale.ROOT);

        String type = singular(seg[0]);
        String id = null;
        String detail = null;
        if (seg.length >= 3 && "patient".equals(seg[1]) && NUMBER.matcher(seg[2]).matches()) {
            // e.g. /emr/visits/patient/9 = this patient's visits
            detail = seg[0];
            type = "patient";
            id = seg[2];
        } else if (seg.length >= 2 && NUMBER.matcher(seg[1]).matches()) {
            id = seg[1];
            if (seg.length >= 3 && WORD.matcher(seg[2]).matches()) detail = seg[2];
        }

        String action;
        switch (verb) {
            case "GET" -> {
                if (id == null) return Optional.empty();
                action = "VIEW";
            }
            case "POST" -> action = detail != null && id != null && !"patient".equals(type) ? postAction(detail) : "CREATE";
            case "PUT", "PATCH" -> action = "UPDATE";
            case "DELETE" -> action = "DELETE";
            default -> { return Optional.empty(); }
        }
        if (status == 401 || status == 403) action = "DENIED_" + action;

        return Optional.of(AuditEvent.builder()
                .action(cut(action, 30)).resourceType(cut(type, 40)).resourceId(id).detail(cut(detail, 60))
                .httpMethod(verb).path(cut(path, 300)).status(status).build());
    }

    private static String postAction(String detail) {
        return switch (detail) {
            case "payments" -> "PAYMENT";
            case "mark-printed" -> "PRINT";
            default -> detail.toUpperCase(Locale.ROOT).replace('-', '_');
        };
    }

    private static String singular(String word) {
        String w = word == null ? "unknown" : word.toLowerCase(Locale.ROOT);
        return switch (w) {
            case "billing" -> "invoice";
            case "staff" -> "staff";
            case "feedback" -> "feedback";
            default -> w.endsWith("ies") ? w.substring(0, w.length() - 3) + "y" : w.endsWith("s") ? w.substring(0, w.length() - 1) : w;
        };
    }

    private static String cut(String value, int max) {
        return value == null ? null : value.length() > max ? value.substring(0, max) : value;
    }
}
