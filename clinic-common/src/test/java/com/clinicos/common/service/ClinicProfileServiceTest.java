package com.clinicos.common.service;

import com.clinicos.common.dto.ClinicProfileDto;
import com.clinicos.common.entity.ClinicProfile;
import com.clinicos.common.entity.ClinicUser;
import com.clinicos.common.exception.RegistrationException;
import com.clinicos.common.repository.ClinicProfileRepository;
import com.clinicos.common.repository.ClinicUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClinicProfileServiceTest {

    private final ClinicProfileRepository profiles = mock(ClinicProfileRepository.class);
    private final ClinicUserRepository users = mock(ClinicUserRepository.class);
    private ClinicProfileService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        ObjectProvider<ClinicProfileRepository> p = mock(ObjectProvider.class);
        ObjectProvider<ClinicUserRepository> u = mock(ObjectProvider.class);
        when(p.getObject()).thenReturn(profiles);
        when(u.getObject()).thenReturn(users);
        service = new ClinicProfileService(p, u);
        when(profiles.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private ClinicProfileDto valid() {
        return ClinicProfileDto.builder().displayName(" Sunrise Clinic ").gstin("29aagcj3874j1z0").upiId("sunrise@okhdfcbank")
                .prescriptionLanguage("KN").bookingEnabled(true).bookingSlug("Sunrise-Clinic").build();
    }

    @Test
    void unsavedProfileIsFilledFromTheAdminRegistration() {
        when(profiles.findById("C1")).thenReturn(Optional.empty());
        when(users.findByClinicId("C1")).thenReturn(List.of(
                ClinicUser.builder().id(2L).role("DOCTOR").clinicName("Wrong").build(),
                ClinicUser.builder().id(3L).role("ADMIN").clinicName("Sunrise Clinic").clinicAddress("Jayanagar").phone("9876543210").email("o@x.in").build()));

        ClinicProfileDto dto = service.get("C1");

        assertThat(dto.getDraft()).isTrue();
        assertThat(dto.getDisplayName()).isEqualTo("Sunrise Clinic");
        assertThat(dto.getAddress()).isEqualTo("Jayanagar");
    }

    @Test
    void saveNormalisesValues() {
        when(profiles.findById("C1")).thenReturn(Optional.empty());
        when(profiles.findByBookingSlug("sunrise-clinic")).thenReturn(Optional.empty());

        ClinicProfileDto saved = service.save("C1", valid(), "owner@x.in");

        assertThat(saved.getDisplayName()).isEqualTo("Sunrise Clinic");
        assertThat(saved.getGstin()).isEqualTo("29AAGCJ3874J1Z0");
        assertThat(saved.getPrescriptionLanguage()).isEqualTo("kn");
        assertThat(saved.getBookingSlug()).isEqualTo("sunrise-clinic");
        assertThat(saved.getDraft()).isFalse();
    }

    @Test
    void rejectsInvalidGstinUpiAndLanguage() {
        when(profiles.findById("C1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.save("C1", valid().toBuilder().gstin("29AAG").build(), "a")).hasMessageContaining("GSTIN");
        assertThatThrownBy(() -> service.save("C1", valid().toBuilder().upiId("nope").build(), "a")).hasMessageContaining("UPI");
        assertThatThrownBy(() -> service.save("C1", valid().toBuilder().prescriptionLanguage("fr").build(), "a")).hasMessageContaining("language");
    }

    @Test
    void bookingLinkMustBeUniqueAcrossClinics() {
        when(profiles.findById("C1")).thenReturn(Optional.empty());
        when(profiles.findByBookingSlug("sunrise-clinic")).thenReturn(Optional.of(ClinicProfile.builder().clinicId("C2").build()));

        assertThatThrownBy(() -> service.save("C1", valid(), "a"))
                .isInstanceOfSatisfying(RegistrationException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void bookingNeedsALinkAndReservedNamesAreRefused() {
        when(profiles.findById("C1")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.save("C1", valid().toBuilder().bookingSlug(null).build(), "a")).hasMessageContaining("booking link");
        assertThatThrownBy(() -> service.save("C1", valid().toBuilder().bookingSlug("admin").build(), "a")).hasMessageContaining("Booking link");
    }

    @Test
    void publicLookupOnlyWhileBookingIsOn() {
        when(profiles.findByBookingSlug("sunrise")).thenReturn(Optional.of(ClinicProfile.builder().clinicId("C1").displayName("Sunrise").bookingSlug("sunrise").bookingEnabled(false).gstin("29AAGCJ3874J1Z0").build()));
        assertThatThrownBy(() -> service.publicBySlug("sunrise"))
                .isInstanceOfSatisfying(RegistrationException.class, e -> assertThat(e.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static String dataUrl(String type, byte[] bytes) {
        return "data:" + type + ";base64," + java.util.Base64.getEncoder().encodeToString(bytes);
    }

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 0x0d};

    @Test
    void brandingIsSavedAndShownOnTheBookingPage() {
        when(profiles.findById("C1")).thenReturn(Optional.empty());
        when(profiles.findByBookingSlug("sunrise-clinic")).thenReturn(Optional.empty());

        ClinicProfileDto saved = service.save("C1", valid().toBuilder().brandColor("#0F766E").bookingTheme("gradient").logo(dataUrl("image/png", PNG)).build(), "admin@x.in");

        assertThat(saved.getBrandColor()).isEqualTo("#0f766e");
        assertThat(saved.getBookingTheme()).isEqualTo("GRADIENT");
        assertThat(saved.getLogo()).startsWith("data:image/png;base64,");

        ClinicProfile row = ClinicProfile.builder().clinicId("C1").displayName("Sunrise").bookingEnabled(true).bookingSlug("sunrise-clinic")
                .brandColor("#0f766e").bookingTheme("GRADIENT").logo(saved.getLogo()).build();
        when(profiles.findByBookingSlug("sunrise-clinic")).thenReturn(Optional.of(row));
        assertThat(service.publicBySlug("sunrise-clinic").getLogo()).isEqualTo(saved.getLogo());
        assertThat(service.publicBySlug("sunrise-clinic").getBrandColor()).isEqualTo("#0f766e");
    }

    @Test
    void defaultsToSoftThemeAndNoLogo() {
        when(profiles.findById("C1")).thenReturn(Optional.empty());
        when(profiles.findByBookingSlug("sunrise-clinic")).thenReturn(Optional.empty());

        ClinicProfileDto saved = service.save("C1", valid().toBuilder().logo("").build(), "admin@x.in");

        assertThat(saved.getBookingTheme()).isEqualTo("SOFT");
        assertThat(saved.getLogo()).isNull();
        assertThat(saved.getBrandColor()).isNull();
    }

    @Test
    void refusesUnsafeOrBrokenBranding() {
        when(profiles.findById("C1")).thenReturn(Optional.empty());
        when(profiles.findByBookingSlug(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save("C1", valid().toBuilder().brandColor("red").build(), "a"))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("brand colour");
        assertThatThrownBy(() -> service.save("C1", valid().toBuilder().bookingTheme("NEON").build(), "a"))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("background");
        assertThatThrownBy(() -> service.save("C1", valid().toBuilder().logo(dataUrl("image/svg+xml", "<svg onload=alert(1)>".getBytes())).build(), "a"))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("PNG, JPG or WebP");
        assertThatThrownBy(() -> service.save("C1", valid().toBuilder().logo(dataUrl("image/png", "<html>".getBytes())).build(), "a"))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("could not be read");
        byte[] big = new byte[ClinicProfileService.MAX_LOGO_BYTES + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        assertThatThrownBy(() -> service.save("C1", valid().toBuilder().logo(dataUrl("image/png", big)).build(), "a"))
                .isInstanceOf(RegistrationException.class).hasMessageContaining("too large");
    }
}
