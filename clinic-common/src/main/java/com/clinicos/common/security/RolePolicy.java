package com.clinicos.common.security;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * What each clinic role may see and do. Used by the gateway (to allow or refuse each API call) and
 * by the web app (to show only the pages a role can use), so the two never drift apart.
 *
 * Only invited staff accounts are restricted. The person who registered the clinic (the owner) and
 * staff with the ADMIN role see everything, whatever their profession.
 */
public final class RolePolicy {

    private RolePolicy() { }

    /** Areas of the app. The web app maps each to its menu items and routes. */
    public enum Page {
        DASHBOARD, PATIENTS, PATIENTS_EDIT, APPOINTMENTS, ONLINE_REQUESTS, QUEUE,
        VISITS, VITALS, PRESCRIPTIONS, PRESCRIPTIONS_WRITE, LAB_REPORTS, ALLERGIES, FILES, SPECIALTY,
        FOLLOWUPS, BILLING, NOTIFICATIONS, FEEDBACK, STAFF, SETTINGS_CLINIC, SUBSCRIPTION, AUDIT_LOG, EXPORT
    }

    private static final Set<Page> ALL = EnumSet.allOf(Page.class);

    /** The pages for a role. Owners (restricted = false) and ADMIN get everything. */
    public static Set<Page> pages(String role, boolean restricted) {
        if (!restricted) return EnumSet.copyOf(ALL);
        return switch (normalize(role)) {
            case "ADMIN" -> EnumSet.copyOf(ALL);
            case "MANAGER" -> {
                Set<Page> s = EnumSet.copyOf(ALL);
                s.remove(Page.SUBSCRIPTION);
                s.remove(Page.AUDIT_LOG);
                yield s;
            }
            case "DOCTOR" -> EnumSet.of(Page.DASHBOARD, Page.PATIENTS, Page.PATIENTS_EDIT, Page.APPOINTMENTS, Page.QUEUE,
                    Page.VISITS, Page.VITALS, Page.PRESCRIPTIONS, Page.PRESCRIPTIONS_WRITE, Page.LAB_REPORTS,
                    Page.ALLERGIES, Page.FILES, Page.SPECIALTY, Page.FOLLOWUPS);
            case "NURSE" -> EnumSet.of(Page.DASHBOARD, Page.PATIENTS, Page.PATIENTS_EDIT, Page.APPOINTMENTS, Page.QUEUE,
                    Page.VITALS, Page.PRESCRIPTIONS, Page.LAB_REPORTS, Page.ALLERGIES, Page.FILES, Page.SPECIALTY, Page.FOLLOWUPS);
            case "RECEPTIONIST" -> EnumSet.of(Page.DASHBOARD, Page.PATIENTS, Page.PATIENTS_EDIT, Page.APPOINTMENTS,
                    Page.ONLINE_REQUESTS, Page.QUEUE, Page.BILLING, Page.FOLLOWUPS, Page.NOTIFICATIONS, Page.FEEDBACK);
            case "LAB_TECHNICIAN" -> EnumSet.of(Page.DASHBOARD, Page.PATIENTS, Page.LAB_REPORTS, Page.FILES);
            case "PHARMACIST" -> EnumSet.of(Page.DASHBOARD, Page.PATIENTS, Page.PRESCRIPTIONS);
            case "BILLING", "ACCOUNTANT" -> EnumSet.of(Page.DASHBOARD, Page.PATIENTS, Page.BILLING);
            default -> EnumSet.of(Page.DASHBOARD);
        };
    }

    private record Rule(Pattern path, boolean writes, boolean reads, List<Page> anyOf) {
        boolean matches(String method, String p) {
            boolean write = !"GET".equals(method) && !"HEAD".equals(method);
            return (write ? writes : reads) && path.matcher(p).matches();
        }
    }

    private static Rule read(String regex, Page... anyOf) { return new Rule(Pattern.compile(regex), false, true, List.of(anyOf)); }
    private static Rule write(String regex, Page... anyOf) { return new Rule(Pattern.compile(regex), true, false, List.of(anyOf)); }
    private static Rule both(String regex, Page... anyOf) { return new Rule(Pattern.compile(regex), true, true, List.of(anyOf)); }
    /** Needed by every signed-in clinic member (an empty list means no page is required). */
    private static Rule anyone(String regex, boolean reads, boolean writes) { return new Rule(Pattern.compile(regex), writes, reads, List.of()); }

    /** First matching rule wins. Paths not listed are refused for restricted staff. */
    private static final List<Rule> RULES = List.of(
            // Clinic details, branding and plan: read by every screen
            anyone("/api/v1/clinic/(profile|me)", true, false),
            write("/api/v1/clinic/profile", Page.SETTINGS_CLINIC),
            both("/api/v1/clinic/booking-requests(/.*)?", Page.ONLINE_REQUESTS),
            anyone("/api/v1/clinic/audit-log/events", false, true),
            both("/api/v1/clinic/audit-log(/.*)?", Page.AUDIT_LOG),
            both("/api/v1/clinic/staff-access(/.*)?", Page.STAFF),
            anyone("/api/v1/subscriptions(/.*)?", true, false),
            write("/api/v1/subscriptions(/.*)?", Page.SUBSCRIPTION),

            read("/api/v1/patients(/.*)?", Page.PATIENTS),
            write("/api/v1/patients(/.*)?", Page.PATIENTS_EDIT),

            read("/api/v1/appointments/(today|day)", Page.APPOINTMENTS, Page.QUEUE),
            both("/api/v1/appointments(/.*)?", Page.APPOINTMENTS),

            write("/api/v1/emr/prescriptions/[^/]+/mark-printed", Page.PRESCRIPTIONS),
            read("/api/v1/emr/prescriptions(/.*)?", Page.PRESCRIPTIONS),
            write("/api/v1/emr/prescriptions(/.*)?", Page.PRESCRIPTIONS_WRITE),
            both("/api/v1/emr/favourite-drugs(/.*)?", Page.PRESCRIPTIONS_WRITE),
            read("/api/v1/emr/vitals(/.*)?", Page.VITALS, Page.VISITS),
            write("/api/v1/emr/vitals(/.*)?", Page.VITALS),
            read("/api/v1/emr/allergies(/.*)?", Page.ALLERGIES, Page.PRESCRIPTIONS, Page.VISITS),
            write("/api/v1/emr/allergies(/.*)?", Page.ALLERGIES),
            both("/api/v1/emr/lab-reports(/.*)?", Page.LAB_REPORTS),
            both("/api/v1/emr/files(/.*)?", Page.FILES),
            both("/api/v1/emr/specialty-exams(/.*)?", Page.SPECIALTY),
            read("/api/v1/emr/visits(/.*)?", Page.VISITS, Page.VITALS),
            read("/api/v1/emr/patients/[^/]+/(history|diagnoses|chronic)", Page.VISITS, Page.PRESCRIPTIONS, Page.VITALS),
            both("/api/v1/emr(/.*)?", Page.VISITS),

            both("/api/v1/followups(/.*)?", Page.FOLLOWUPS),
            both("/api/v1/billing(/.*)?", Page.BILLING),
            both("/api/v1/notifications(/.*)?", Page.NOTIFICATIONS),
            both("/api/v1/feedback(/.*)?", Page.FEEDBACK),

            // Doctor lists and schedules feed the appointment calendar and prescription letterheads
            anyone("/api/v1/staff/(doctors|[0-9]+|schedules/.*)", true, false),
            both("/api/v1/staff(/.*)?", Page.STAFF)
    );

    /** Paths this policy does not govern: sign-in, public pages, super-admin tools (checked elsewhere). */
    public static boolean governs(String path) {
        return path.startsWith("/api/v1/")
                && !path.startsWith("/api/v1/auth/")
                && !path.startsWith("/api/v1/public/")
                && !path.startsWith("/api/v1/admin/")
                && !path.startsWith("/api/v1/feedback/token/")
                && !path.equals("/api/v1/feedback/public");
    }

    /**
     * Why a restricted staff member may not make this call, or empty when they may.
     * Owners and ADMIN are never refused here.
     */
    public static Optional<String> refusal(String role, boolean restricted, String method, String path) {
        if (!restricted || !governs(path)) return Optional.empty();
        String m = method == null ? "GET" : method.toUpperCase(Locale.ROOT);
        if ("OPTIONS".equals(m)) return Optional.empty();
        Set<Page> pages = pages(role, true);
        if (pages.containsAll(ALL)) return Optional.empty();
        for (Rule rule : RULES) {
            if (!rule.matches(m, path)) continue;
            if (rule.anyOf().isEmpty() || rule.anyOf().stream().anyMatch(pages::contains)) return Optional.empty();
            return Optional.of(message(role, rule.anyOf().get(0)));
        }
        return Optional.of(message(role, null));
    }

    static String message(String role, Page needed) {
        String what = needed == null ? "this part of CliniSys One" : switch (needed) {
            case BILLING -> "billing";
            case STAFF -> "staff management";
            case SETTINGS_CLINIC -> "clinic settings";
            case SUBSCRIPTION -> "the subscription";
            case AUDIT_LOG -> "the activity log";
            case PRESCRIPTIONS_WRITE -> "writing prescriptions";
            case PATIENTS_EDIT -> "editing patient details";
            case ONLINE_REQUESTS -> "online booking requests";
            case NOTIFICATIONS -> "notifications";
            case FEEDBACK -> "patient feedback";
            case APPOINTMENTS, QUEUE -> "appointments";
            case FOLLOWUPS -> "follow-ups";
            case VISITS, VITALS, LAB_REPORTS, ALLERGIES, FILES, SPECIALTY, PRESCRIPTIONS -> "clinical records";
            default -> "this part of CliniSys One";
        };
        return "Your role (" + label(role) + ") doesn't include " + what + ". Ask the clinic owner if you need access.";
    }

    public static String label(String role) {
        String r = normalize(role);
        if (r.isEmpty()) return "Staff";
        String words = r.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    /** Roles an owner can give an invited staff member. */
    public static final Set<String> STAFF_ROLES = Set.of("ADMIN", "MANAGER", "DOCTOR", "NURSE", "RECEPTIONIST",
            "LAB_TECHNICIAN", "PHARMACIST", "BILLING", "ACCOUNTANT");

    public static String normalize(String role) {
        return role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
    }
}
