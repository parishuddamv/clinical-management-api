package com.clinicos.common.service;

import com.clinicos.common.dto.BookingRequestDto;
import com.clinicos.common.dto.PublicDoctorDto;
import com.clinicos.common.entity.BookingRequest;
import com.clinicos.common.entity.ClinicProfile;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.repository.BookingRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Appointment requests from public booking pages. Patients ask for a day and time; the front desk
 * confirms by booking the real appointment. Limits stop one phone or one network flooding a clinic.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    static final int MAX_PER_PHONE_PER_DAY = 3;
    static final int MAX_PER_IP_PER_HOUR = 10;
    static final int MAX_DAYS_AHEAD = 60;
    private static final Set<String> TIME_SLOTS = Set.of("MORNING", "AFTERNOON", "EVENING");
    private static final Pattern CLOCK = Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d$");
    private static final Pattern INDIAN_MOBILE = Pattern.compile("^[6-9]\\d{9}$");
    private static final String REF_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ObjectProvider<BookingRequestRepository> requests;
    private final ClinicProfileService profiles;
    private Clock clock = Clock.systemDefaultZone();

    void setClock(Clock clock) {
        this.clock = clock;
    }

    /** Public: a patient asks for an appointment. `doctors` is the clinic's current doctor list. */
    @Transactional
    public BookingRequestDto request(String slug, BookingRequestDto input, String ip, List<PublicDoctorDto> doctors) {
        ClinicProfile clinic = profiles.bookableBySlug(slug);
        if (input == null) throw bad("Please fill in the form.");
        if (input.getWebsite() != null && !input.getWebsite().isBlank()) {
            // Spam bot: answer as if it worked, store nothing
            return BookingRequestDto.builder().reference("RECEIVED").status("NEW").build();
        }
        String name = text(input.getPatientName(), 100);
        if (name == null || name.length() < 2) throw bad("Enter the patient's name.");
        String phone = mobile(input.getPhone());
        if (!Boolean.TRUE.equals(input.getConsent())) throw bad("Please agree to be contacted about this appointment.");
        LocalDate today = LocalDate.now(clock);
        LocalDate date = input.getPreferredDate();
        if (date == null || date.isBefore(today) || date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw bad("Choose a date from today up to " + MAX_DAYS_AHEAD + " days ahead.");
        }
        String time = input.getPreferredTime() == null ? "" : input.getPreferredTime().trim().toUpperCase(Locale.ROOT);
        if (!TIME_SLOTS.contains(time) && !CLOCK.matcher(time).matches()) throw bad("Choose morning, afternoon or evening.");
        Integer age = input.getAge();
        if (age != null && (age < 0 || age > 120)) throw bad("Enter an age between 0 and 120.");
        String gender = input.getGender() == null || input.getGender().isBlank() ? null : input.getGender().trim().toUpperCase(Locale.ROOT);
        if (gender != null && !Set.of("MALE", "FEMALE", "OTHER").contains(gender)) throw bad("Choose a valid gender.");

        Long doctorId = input.getDoctorId();
        String doctorName = null;
        if (doctorId != null) {
            doctorName = doctors.stream().filter(d -> doctorId.equals(d.getId())).map(PublicDoctorDto::getName).findFirst()
                    .orElseThrow(() -> bad("Choose a doctor from the list."));
        }

        BookingRequestRepository repo = requests.getObject();
        LocalDateTime now = LocalDateTime.now(clock);
        if (ip != null && repo.countByRequesterIpAndCreatedAtAfter(ip, now.minusHours(1)) >= MAX_PER_IP_PER_HOUR) {
            throw new RegistrationException(HttpStatus.TOO_MANY_REQUESTS, "Too many requests from this network. Please call the clinic.");
        }
        if (repo.countByClinicIdAndPhoneAndCreatedAtAfter(clinic.getClinicId(), phone, now.minusDays(1)) >= MAX_PER_PHONE_PER_DAY) {
            throw new RegistrationException(HttpStatus.TOO_MANY_REQUESTS, "We already have your requests for today. The clinic will call you soon.");
        }

        BookingRequest saved = repo.save(BookingRequest.builder()
                .clinicId(clinic.getClinicId()).reference(newReference(repo)).patientName(name).phone(phone).age(age).gender(gender)
                .preferredDate(date).preferredTime(time).doctorId(doctorId).doctorName(doctorName)
                .reason(text(input.getReason(), 300)).requesterIp(ip == null ? null : ip.substring(0, Math.min(64, ip.length())))
                .createdAt(now).build());
        // The patient only gets their reference back, nothing else about the clinic's data
        return BookingRequestDto.builder().reference(saved.getReference()).status(saved.getStatus().name())
                .preferredDate(saved.getPreferredDate()).preferredTime(saved.getPreferredTime()).doctorName(saved.getDoctorName()).build();
    }

    @Transactional(readOnly = true)
    public Page<BookingRequestDto> list(String clinicId, String status, int page, int size) {
        if (page < 0 || size < 1 || size > 100) throw bad("page must be 0 or more and size 1-100.");
        BookingRequestRepository repo = requests.getObject();
        PageRequest p = PageRequest.of(page, size);
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return repo.findByClinicIdOrderByCreatedAtDesc(clinicId, p).map(BookingRequestDto::from);
        }
        return repo.findByClinicIdAndStatusOrderByPreferredDateAscCreatedAtAsc(clinicId, status(status), p).map(BookingRequestDto::from);
    }

    @Transactional(readOnly = true)
    public long countNew(String clinicId) {
        return requests.getObject().countByClinicIdAndStatus(clinicId, BookingRequest.Status.NEW);
    }

    /** The front desk confirms (with the appointment it booked), declines or cancels a request. */
    @Transactional
    public BookingRequestDto update(String clinicId, Long id, String status, Long appointmentId, String actor) {
        BookingRequest b = requests.getObject().findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new RegistrationException(HttpStatus.NOT_FOUND, "Booking request not found."));
        BookingRequest.Status next = status(status);
        if (next == BookingRequest.Status.NEW) throw bad("Choose confirm, decline or cancel.");
        if (next == BookingRequest.Status.CONFIRMED && appointmentId == null) throw bad("Book the appointment first, then confirm the request.");
        b.setStatus(next);
        b.setAppointmentId(next == BookingRequest.Status.CONFIRMED ? appointmentId : b.getAppointmentId());
        b.setHandledBy(actor);
        b.setHandledAt(LocalDateTime.now(clock));
        return BookingRequestDto.from(requests.getObject().save(b));
    }

    private static BookingRequest.Status status(String value) {
        try {
            return BookingRequest.Status.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw bad("Unknown status.");
        }
    }

    /** "+91 98765-43210" -> "9876543210" */
    static String mobile(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) digits = digits.substring(2);
        if (digits.length() == 11 && digits.startsWith("0")) digits = digits.substring(1);
        if (!INDIAN_MOBILE.matcher(digits).matches()) throw bad("Enter a 10-digit Indian mobile number.");
        return digits;
    }

    private static String newReference(BookingRequestRepository repo) {
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder ref = new StringBuilder("BK-");
            for (int i = 0; i < 6; i++) ref.append(REF_CHARS.charAt(RANDOM.nextInt(REF_CHARS.length())));
            if (!repo.existsByReference(ref.toString())) return ref.toString();
        }
        throw new IllegalStateException("Could not create a booking reference");
    }

    private static String text(String value, int max) {
        if (value == null || value.isBlank()) return null;
        String t = value.trim().replaceAll("\\s+", " ");
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static RegistrationException bad(String message) {
        return new RegistrationException(HttpStatus.BAD_REQUEST, message);
    }
}
