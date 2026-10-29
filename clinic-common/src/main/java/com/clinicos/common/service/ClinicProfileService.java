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
    public static final Set<String> THEMES = Set.of("PLAIN", "SOFT", "GRADIENT", "PATTERN");
    // Kept under the gateway's 256 KB request body limit once base64-encoded with the rest of the profile.
    static final int MAX_LOGO_BYTES = 150 * 1024;
    private static final Pattern HEX = Pattern.compile("^#[0-9a-f]{6}$");
    private static final Pattern LOGO = Pattern.compile("^data:(image/(?:png|jpeg|webp));base64,([A-Za-z0-9+/]+={0,2})$");
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
        String color = optional(input.getBrandColor(), "Brand colour", 7);
        if (color != null) {
            color = color.toLowerCase(Locale.ROOT);
            if (!HEX.matcher(color).matches()) throw bad("Choose a brand colour such as #0f766e.");
        }
        p.setBrandColor(color);
        String theme = input.getBookingTheme() == null || input.getBookingTheme().isBlank() ? "SOFT" : input.getBookingTheme().trim().toUpperCase(Locale.ROOT);
        if (!THEMES.contains(theme)) throw bad("Choose a booking page background.");
        p.setBookingTheme(theme);
        p.setLogo(logo(input.getLogo()));

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
                .address(p.getAddress()).phone(p.getPhone()).workingHours(p.getWorkingHours()).bookingIntro(p.getBookingIntro())
                .brandColor(p.getBrandColor()).bookingTheme(p.getBookingTheme()).logo(p.getLogo()).build();
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

    /** Accepts a PNG, JPEG or WebP data URL whose bytes really are that image type. SVG is refused. */
    static String logo(String value) {
        if (value == null || value.isBlank()) return null;
        java.util.regex.Matcher m = LOGO.matcher(value.trim());
        if (!m.matches()) throw bad("Upload the logo as a PNG, JPG or WebP image.");
        byte[] bytes;
        try {
            bytes = java.util.Base64.getDecoder().decode(m.group(2));
        } catch (IllegalArgumentException e) {
            throw bad("The logo file could not be read. Try another image.");
        }
        if (bytes.length > MAX_LOGO_BYTES) throw bad("The logo is too large. Use an image under 150 KB.");
        if (!looksLike(m.group(1), bytes)) throw bad("The logo file could not be read. Try another image.");
        return value.trim();
    }

    private static boolean looksLike(String type, byte[] b) {
        return switch (type) {
            case "image/png" -> b.length > 8 && (b[0] & 0xff) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
            case "image/jpeg" -> b.length > 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8;
            case "image/webp" -> b.length > 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
            default -> false;
        };
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
