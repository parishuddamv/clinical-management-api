package com.clinicos.common.service;

import com.clinicos.common.dto.ClinicProfileDto;
import com.clinicos.common.dto.PublicClinicDto;
import com.clinicos.common.entity.ClinicProfile;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.repository.ClinicProfileRepository;
import com.clinicos.common.repository.ClinicUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Clinic profile: letterhead details, GSTIN, UPI and booking settings. Until an admin saves it, the
 * profile is filled from the clinic's registration so prescriptions and invoices still show a name.
 */
@Service
@RequiredArgsConstructor
public class ClinicProfileService {

    public static final Set<String> LANGUAGES = Set.of("en", "hi", "kn", "ta", "te", "mr", "bn", "gu", "ml", "pa");
    private static final Pattern GSTIN = Pattern.compile("^\\d{2}[A-Z]{5}\\d{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");
    private static final Pattern UPI = Pattern.compile("^[a-zA-Z0-9.\\-_]{2,256}@[a-zA-Z][a-zA-Z0-9]{1,63}$");
    private static final Pattern SLUG = Pattern.compile("^[a-z0-9](?:[a-z0-9-]{1,48}[a-z0-9])$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()-]{7,20}$");
    private static final Set<String> RESERVED_SLUGS = Set.of("admin", "api", "app", "book", "clinic", "clinisys", "help", "login", "support");

    private final ObjectProvider<ClinicProfileRepository> profiles;
    private final ObjectProvider<ClinicUserRepository> users;

    @Transactional(readOnly = true)
    public ClinicProfileDto get(String clinicId) {
        return profiles.getObject().findById(clinicId)
                .map(p -> ClinicProfileDto.from(p, false))
                .orElseGet(() -> ClinicProfileDto.from(defaults(clinicId), true));
    }

    @Transactional
    public ClinicProfileDto save(String clinicId, ClinicProfileDto input, String actor) {
        if (input == null) throw bad("Profile details are required.");
        ClinicProfileRepository repo = profiles.getObject();
        ClinicProfile p = repo.findById(clinicId).orElseGet(() -> ClinicProfile.builder().clinicId(clinicId).build());

        p.setDisplayName(required(input.getDisplayName(), "Clinic name", 200));
        p.setTagline(optional(input.getTagline(), "Tagline", 200));
        p.setAddress(optional(input.getAddress(), "Address", 500));
        p.setPhone(matching(input.getPhone(), PHONE, "Enter a valid phone number."));
        p.setEmail(matching(input.getEmail(), Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"), "Enter a valid email address."));
        String gstin = optional(input.getGstin(), "GSTIN", 15);
        p.setGstin(gstin == null ? null : matching(gstin.toUpperCase(Locale.ROOT), GSTIN, "Enter a valid 15-character GSTIN."));
        p.setRegistrationNumber(optional(input.getRegistrationNumber(), "Clinic registration number", 100));
        p.setWorkingHours(optional(input.getWorkingHours(), "Working hours", 200));
        p.setUpiId(matching(input.getUpiId(), UPI, "Enter a UPI ID such as clinicname@okhdfcbank."));
        p.setUpiPayeeName(optional(input.getUpiPayeeName(), "UPI payee name", 100));
        String language = input.getPrescriptionLanguage() == null ? "en" : input.getPrescriptionLanguage().trim().toLowerCase(Locale.ROOT);
        if (!LANGUAGES.contains(language)) throw bad("Choose a supported prescription language.");
        p.setPrescriptionLanguage(language);
        p.setPrescriptionFooter(optional(input.getPrescriptionFooter(), "Prescription footer", 300));
        p.setWhatsappNumber(matching(input.getWhatsappNumber(), PHONE, "Enter a valid WhatsApp number."));
        p.setBookingIntro(optional(input.getBookingIntro(), "Booking page message", 500));

        String slug = optional(input.getBookingSlug(), "Booking link", 50);
        if (slug != null) {
            slug = slug.toLowerCase(Locale.ROOT);
            if (!SLUG.matcher(slug).matches() || RESERVED_SLUGS.contains(slug)) {
                throw bad("Booking link may use lowercase letters, numbers and hyphens (3–50 characters).");
            }
            String finalSlug = slug;
            repo.findByBookingSlug(slug).filter(other -> !other.getClinicId().equals(clinicId)).ifPresent(other -> {
                throw new RegistrationException(HttpStatus.CONFLICT, "The booking link \"" + finalSlug + "\" is already taken. Try another.");
            });
        }
        p.setBookingSlug(slug);
        boolean booking = Boolean.TRUE.equals(input.getBookingEnabled());
        if (booking && slug == null) throw bad("Choose a booking link before turning on online booking.");
        p.setBookingEnabled(booking);

        p.setUpdatedAt(LocalDateTime.now());
        p.setUpdatedBy(actor);
        return ClinicProfileDto.from(repo.save(p), false);
    }

    /** Booking page details, only while the clinic has online booking turned on. */
    @Transactional(readOnly = true)
    public PublicClinicDto publicBySlug(String slug) {
        ClinicProfile p = bookableBySlug(slug);
        return PublicClinicDto.builder().slug(p.getBookingSlug()).displayName(p.getDisplayName()).tagline(p.getTagline())
                .address(p.getAddress()).phone(p.getPhone()).workingHours(p.getWorkingHours()).bookingIntro(p.getBookingIntro()).build();
    }

    @Transactional(readOnly = true)
    public ClinicProfile bookableBySlug(String slug) {
        String key = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        return profiles.getObject().findByBookingSlug(key)
                .filter(p -> Boolean.TRUE.equals(p.getBookingEnabled()))
                .orElseThrow(() -> new RegistrationException(HttpStatus.NOT_FOUND, "This booking page is not available."));
    }

    private ClinicProfile defaults(String clinicId) {
        ClinicUser owner = users.getObject().findByClinicId(clinicId).stream()
                .sorted(Comparator.comparing((ClinicUser u) -> !"ADMIN".equalsIgnoreCase(u.getRole())).thenComparing(ClinicUser::getId))
                .findFirst().orElse(null);
        return ClinicProfile.builder()
                .clinicId(clinicId)
                .displayName(owner != null && owner.getClinicName() != null ? owner.getClinicName() : "Your clinic")
                .address(owner == null ? null : owner.getClinicAddress())
                .phone(owner == null ? null : (owner.getClinicPhone() != null ? owner.getClinicPhone() : owner.getPhone()))
                .email(owner == null ? null : owner.getEmail())
                .build();
    }

    private static String optional(String value, String label, int max) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.trim();
        if (trimmed.length() > max) throw bad(label + " must be " + max + " characters or fewer.");
        return trimmed;
    }

    private static String required(String value, String label, int max) {
        String trimmed = optional(value, label, max);
        if (trimmed == null) throw bad(label + " is required.");
        return trimmed;
    }

    private static String matching(String value, Pattern pattern, String message) {
        String trimmed = optional(value, "Value", 256);
        if (trimmed != null && !pattern.matcher(trimmed).matches()) throw bad(message);
        return trimmed;
    }

    private static RegistrationException bad(String message) {
        return new RegistrationException(HttpStatus.BAD_REQUEST, message);
    }
}
